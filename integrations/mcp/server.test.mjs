import {test} from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import {fileURLToPath} from "node:url";
import {Client} from "@modelcontextprotocol/sdk/client/index.js";
import {StdioClientTransport} from "@modelcontextprotocol/sdk/client/stdio.js";
import {BillVaultClient, expenseSummary} from "./client.mjs";

const token="a".repeat(64);
test("rejects unsafe targets and does not expose private note fields", () => {
  assert.throws(()=>new BillVaultClient({BILLVAULT_MODE:"web",BILLVAULT_URL:"http://example.com"}));
  assert.throws(()=>new BillVaultClient({BILLVAULT_URL:"http://user:pass@127.0.0.1",BILLVAULT_MCP_TOKEN:token}));
  assert.throws(()=>new BillVaultClient({BILLVAULT_MCP_TOKEN:"short"}));
  const page=expenseSummary({content:[{id:1,name:"Demo",notes:"private",description:"private",paymentMethod:"private"}]});
  assert.equal(page.content[0].notes,undefined);
  assert.equal(page.content[0].paymentMethod,undefined);
});

test("web session login retains rotated cookies and refreshed CSRF", async () => {
  const calls=[];
  const replies=[
    new Response(JSON.stringify({token:"before",headerName:"X-CSRF-TOKEN"}),{headers:{"Set-Cookie":"JSESSIONID=before; HttpOnly"}}),
    new Response(null,{status:204,headers:{"Set-Cookie":"JSESSIONID=after; HttpOnly"}}),
    new Response(JSON.stringify({token:"after",headerName:"X-CSRF-TOKEN"})),
    new Response(JSON.stringify({previewId:"fixture"}))
  ];
  const client=new BillVaultClient({BILLVAULT_MODE:"web",BILLVAULT_URL:"https://vault.example",BILLVAULT_USERNAME:"fixture",BILLVAULT_PASSWORD:"fixture-password"},async (url,options)=>{calls.push({url,...options});return replies.shift();});
  await client.request("/api/imports/preview","POST",{source:"demo",expenses:[]});
  assert.equal(calls[1].headers["X-CSRF-TOKEN"],"before");
  assert.equal(calls[2].headers.Cookie,"JSESSIONID=after");
  assert.equal(calls[3].headers["X-CSRF-TOKEN"],"after");
  assert.equal(calls[3].redirect,"error");
});

test("real SDK stdio handshake, discovery, schema rejection and tool routing", async () => {
  const calls=[];
  const backend=http.createServer(async (req,res)=>{
    assert.equal(req.headers.authorization,`Bearer ${token}`);
    let body="";for await(const chunk of req) body+=chunk;
    calls.push({url:req.url,body:body?JSON.parse(body):null});
    res.setHeader("Content-Type","application/json");
    res.end(JSON.stringify(req.url==="/api/categories" ? [{id:1,name:"Other",color:"#888888"}] :
      req.url.startsWith("/api/expenses?") ? {content:[{id:1,name:"Demo",notes:"never expose"}],page:0} :
      req.url.endsWith("/preview") ? {previewId:"c3630bee-ae03-44dc-8705-2d758c13419d",rows:[]} : {createdIds:[1],skipped:0}));
  });
  await new Promise(resolve=>backend.listen(0,"127.0.0.1",resolve));
  const transport=new StdioClientTransport({command:process.execPath,args:[fileURLToPath(new URL("server.mjs",import.meta.url))],
    env:{...process.env,BILLVAULT_MODE:"desktop",BILLVAULT_URL:`http://127.0.0.1:${backend.address().port}`,BILLVAULT_MCP_TOKEN:token},stderr:"pipe"});
  const client=new Client({name:"billvault-test",version:"1.0.0"});
  try {
    await client.connect(transport);
    assert.equal((await client.listTools()).tools.length,4);
    const categories=await client.callTool({name:"billvault_list_categories",arguments:{}});
    assert.equal(JSON.parse(categories.content[0].text)[0].name,"Other");
    const expenses=await client.callTool({name:"billvault_list_expenses",arguments:{}});
    assert.equal(JSON.parse(expenses.content[0].text).content[0].notes,undefined);
    const expense={name:"Demo",kind:"SUBSCRIPTION",categoryId:1,amount:"14.99",currency:"USD",frequency:"MONTHLY",startDate:"2026-11-01",nextDueDate:"2026-12-01"};
    const invalid=await client.callTool({name:"billvault_preview_import",arguments:{source:"example",expenses:[{...expense,amount:14.99}]}});
    assert.equal(invalid.isError,true);
    assert.equal(calls.length,2);
    const preview=await client.callTool({name:"billvault_preview_import",arguments:{source:"example",expenses:[expense]}});
    assert.equal(preview.isError,undefined);
    const previewId=JSON.parse(preview.content[0].text).previewId;
    const commit=await client.callTool({name:"billvault_commit_import",arguments:{previewId,confirmed:true}});
    assert.equal(JSON.parse(commit.content[0].text).createdIds[0],1);
    assert.equal(calls.at(-1).body.confirmed,true);
  } finally {await client.close(); await new Promise(resolve=>backend.close(resolve));}
});
