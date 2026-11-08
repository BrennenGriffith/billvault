import { useEffect, useState, type FormEvent } from "react";
import {
  LayoutDashboard,
  Repeat,
  Receipt,
  CalendarDays,
  History,
  ChartNoAxesCombined,
  Settings as SettingsIcon,
  Plus,
  Search,
  Bell,
  ArrowUpRight,
  ArrowDownLeft,
  Wallet,
  ChevronLeft,
  ChevronRight,
  LogOut,
  ShieldCheck,
  Check,
  Sun,
  Moon,
  Download,
} from "lucide-react";
import { api, ApiError, login, logout } from "./api";
import { money, today, dateLabel, title } from "./format";
import {
  Field,
  Empty,
  ExpenseTable,
  Stat,
  TrendChart,
  Modal,
} from "./components";
import ExpenseForm from "./ExpenseForm";
import type {
  Expense,
  ExpenseInput,
  Category,
  Dashboard,
  Settings,
  Payment,
  Notice,
  Page,
  CalendarItem,
  ReportTotal,
} from "./types";
const navigation = [
  ["Dashboard", LayoutDashboard],
  ["Subscriptions", Repeat],
  ["Bills", Receipt],
  ["Upcoming", CalendarDays],
  ["Payment history", History],
  ["Reports", ChartNoAxesCombined],
  ["Settings", SettingsIcon],
] as const;
export default function App() {
  const [user, setUser] = useState<string | null>(null),
    [checking, setChecking] = useState(true),
    [tab, setTab] = useState("Dashboard");
  const [dashboard, setDashboard] = useState<Dashboard | null>(null),
    [categories, setCategories] = useState<Category[]>([]),
    [settings, setSettings] = useState<Settings>({
      currency: "USD",
      theme: "light",
      reminderDays: 3,
    });
  const [expenses, setExpenses] = useState<Page<Expense>>({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }),
    [payments, setPayments] = useState<Payment[]>([]),
    [notices, setNotices] = useState<Notice[]>([]),
    [calendar, setCalendar] = useState<CalendarItem[]>([]);
  const [reportTotals, setReportTotals] = useState<ReportTotal[]>([]);
  const [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [category, setCategory] = useState(""),
    [sort, setSort] = useState("nextDueDate"),
    [page, setPage] = useState(0),
    [month, setMonth] = useState(today().slice(0, 7));
  const [loading, setLoading] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [showNotices, setShowNotices] = useState(false),
    [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState<ExpenseInput | null>(null),
    [editId, setEditId] = useState<number | null>(null),
    [paying, setPaying] = useState<Expense | null>(null),
    [deleting, setDeleting] = useState<number | null>(null),
    [reminder, setReminder] = useState<{
      id: number;
      daysBefore: number;
      enabled: boolean;
    } | null>(null);
  const [payAmount, setPayAmount] = useState(""),
    [payDate, setPayDate] = useState(today()),
    [payNotes, setPayNotes] = useState(""),
    [username, setUsername] = useState("admin"),
    [password, setPassword] = useState("");
  function refresh() {
    setRevision((r) => r + 1);
  }
  useEffect(() => {
    api<{ username: string }>("/auth/me")
      .then((r) => setUser(r.username))
      .catch((e) => {
        if (!(e instanceof ApiError && e.status === 401))
          setError("The backend is unavailable. Start Spring Boot and retry.");
      })
      .finally(() => setChecking(false));
  }, []);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(""), 4500);
    return () => clearTimeout(timer);
  }, [toast]);
  useEffect(() => {
    document.documentElement.dataset.theme = settings.theme;
  }, [settings.theme]);
  useEffect(() => {
    if (!user) return;
    let alive = true;
    setLoading(true);
    setError("");
    const query = new URLSearchParams({ page: String(page), size: "10", sort });
    if (search) query.set("search", search);
    if (status) query.set("status", status);
    if (category) query.set("categoryId", category);
    if (tab === "Bills") query.set("kind", "BILL");
    if (tab === "Subscriptions") query.set("kind", "SUBSCRIPTION");
    const end = new Date(
      Number(month.slice(0, 4)),
      Number(month.slice(5, 7)),
      0,
    ).getDate();
    Promise.all([
      api<Dashboard>("/dashboard"),
      api<Category[]>("/categories"),
      api<Settings>("/settings"),
      api<Page<Expense>>("/expenses?" + query),
      api<Payment[]>("/payments"),
      api<Notice[]>("/notifications"),
      api<ReportTotal[]>(
        `/reports/summary?year=${month.slice(0, 4)}&month=${Number(month.slice(5, 7))}`,
      ),
      api<CalendarItem[]>(`/calendar?from=${month}-01&to=${month}-${end}`),
    ])
      .then(([d, c, s, e, p, n, r, k]) => {
        if (alive) {
          setDashboard(d);
          setCategories(c);
          setSettings(s);
          setExpenses(e);
          setPayments(p);
          setNotices(n);
          setReportTotals(r);
          setCalendar(k);
        }
      })
      .catch((e) => {
        if (alive) {
          setError(e.message);
          if (e.status === 401) setUser(null);
        }
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [user, tab, search, status, category, sort, page, month, revision]);
  async function act(work: () => Promise<unknown>, message: string) {
    setBusy(true);
    setError("");
    try {
      await work();
      setToast(message);
      refresh();
      return true;
    } catch (e) {
      setError((e as Error).message);
      return false;
    } finally {
      setBusy(false);
    }
  }
  function add() {
    setEditId(null);
    setEditing({
      name: "",
      description: "",
      kind: tab === "Bills" ? "BILL" : "SUBSCRIPTION",
      categoryId: categories[0]?.id || 0,
      amount: 0,
      currency: settings.currency,
      frequency: "MONTHLY",
      customInterval: null,
      customUnit: null,
      startDate: today(),
      nextDueDate: today(),
      endDate: null,
      paymentMethod: "",
      websiteUrl: "",
      status: "ACTIVE",
      variableAmount: false,
      notes: "",
    });
  }
  function edit(e: Expense) {
    setEditId(e.id);
    setEditing({ ...e });
  }
  function openPay(e: Expense) {
    setPaying(e);
    setPayAmount(String(e.amount));
    setPayDate(today());
    setPayNotes("");
  }
  async function signIn(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(username, password);
      setPassword("");
      setUser(username);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  function changeTab(name: string) {
    setTab(name);
    setPage(0);
    setSearch("");
    setStatus("");
    setCategory("");
  }
  const currency = settings.currency,
    totals = dashboard?.totals.find((t) => t.currency === currency),
    breakdown =
      dashboard?.categories.filter((c) => c.currency === currency) || [],
    trends = dashboard?.trends.filter((t) => t.currency === currency) || [];
  const heading =
    tab === "Dashboard"
      ? "Your money, in view."
      : tab === "Upcoming"
        ? "Stay one step ahead."
        : tab === "Reports"
          ? "The bigger picture."
          : tab === "Settings"
            ? "Make it yours."
            : tab;
  const descriptions: Record<string, string> = {
    Dashboard: "A little clarity for everything that comes around.",
    Subscriptions: "All your memberships, without the mental load.",
    Bills: "Keep your household expenses in one place.",
    Upcoming: "Projected due dates for your active recurring expenses.",
    "Payment history": "A clear record of what you actually paid.",
    Reports: "Projected costs and recorded payments, clearly separated.",
    Settings: "Your preferences and recurring expense defaults.",
  };
  const rows = (items: Expense[], compact = false) => (
    <ExpenseTable
      items={items}
      compact={compact}
      onAdd={add}
      onEdit={edit}
      onPay={openPay}
    />
  );
  if (checking) return <main className="boot">Opening your vault…</main>;
  if (!user)
    return (
      <main className="login-screen">
        <section className="login-story">
          <div className="logo">
            <span>B</span> BillVault
          </div>
          <div>
            <p className="eyebrow">LESS MENTAL LOAD. MORE CLARITY.</p>
            <h1>
              Every bill.
              <br />
              One clear picture.
            </h1>
            <p>
              A quieter way to keep track of the things
              <br />
              that come around every month.
            </p>
          </div>
          <span className="security-note">
            <ShieldCheck size={18} /> Your records. Your private vault.
          </span>
        </section>
        <section className="login-form">
          <form onSubmit={signIn}>
            <div className="empty-icon">
              <Wallet size={28} />
            </div>
            <h2>Welcome to your vault</h2>
            <p>Sign in to see where your money goes.</p>
            {error && (
              <div role="alert" className="error">
                {error}
              </div>
            )}
            <Field label="Username">
              <input
                autoComplete="username"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </Field>
            <Field label="Password">
              <input
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </Field>
            <button className="primary full" disabled={busy}>
              {busy ? "Signing in…" : "Sign in"}
              <ArrowUpRight size={18} />
            </button>
            <small className="muted">
              Use the administrator credentials configured on your server.
            </small>
          </form>
        </section>
      </main>
    );
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="logo" href="#" onClick={() => changeTab("Dashboard")}>
          <span>B</span> BillVault<span className="logo-dot">.</span>
        </a>
        <div className="workspace">
          <span className="workspace-icon">
            <Wallet size={18} />
          </span>
          <div>
            <strong>Personal vault</strong>
            <small>Your financial home</small>
          </div>
          <ShieldCheck size={15} />
        </div>
        <p className="nav-label">WORKSPACE</p>
        <nav>
          {navigation.map(([name, Icon]) => (
            <button
              key={name}
              className={tab === name ? "nav-item selected" : "nav-item"}
              onClick={() => changeTab(name)}
            >
              <Icon size={19} />
              {name}
              {name === "Upcoming" && !!dashboard?.upcoming7.length && (
                <span className="nav-count">{dashboard.upcoming7.length}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="vault-note">
            <ShieldCheck size={22} />
            <strong>A little peace of mind.</strong>
            <p>
              Your recurring expenses,
              <br />
              organized in one place.
            </p>
          </div>
          <button
            className="profile"
            onClick={() => {
              void act(async () => {
                await logout();
                setUser(null);
              }, "Signed out");
            }}
          >
            <span className="avatar">{user.slice(0, 1).toUpperCase()}</span>
            <div>
              <strong>{user}</strong>
              <small>Administrator</small>
            </div>
            <LogOut size={17} />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div className="breadcrumb">
            Personal vault <span>/</span> <strong>{tab}</strong>
          </div>
          <div className="top-actions">
            <span className="private-label">
              <span /> Private workspace
            </span>
            <button
              className="icon-button"
              title="Toggle theme"
              disabled={busy}
              onClick={() => {
                void act(
                  () =>
                    api("/settings", "PUT", {
                      ...settings,
                      theme: settings.theme === "light" ? "dark" : "light",
                    }),
                  "Theme updated",
                );
              }}
            >
              {settings.theme === "light" ? (
                <Moon size={18} />
              ) : (
                <Sun size={18} />
              )}
            </button>
            <button
              className="icon-button bell"
              aria-label="Notifications"
              onClick={() => setShowNotices(!showNotices)}
            >
              <Bell size={19} />
              {notices.some((n) => !n.read) && <i />}
            </button>
            <span className="avatar small">
              {user.slice(0, 1).toUpperCase()}
            </span>
          </div>
        </header>
        <main className="content">
          <div className="page-heading">
            <div>
              <p className="eyebrow">
                {tab === "Dashboard"
                  ? new Date().toLocaleDateString("en-US", {
                      weekday: "long",
                      month: "long",
                      day: "numeric",
                    })
                  : "YOUR PERSONAL VAULT"}
              </p>
              <h1>{heading}</h1>
              <p>{descriptions[tab]}</p>
            </div>
            {tab !== "Settings" && (
              <button className="primary" onClick={add}>
                <Plus size={18} /> Add expense
              </button>
            )}
          </div>
          {error && (
            <div className="error" role="alert">
              {error}
              <button className="subtle" onClick={refresh}>
                Retry
              </button>
            </div>
          )}
          {loading && (
            <div className="loading" role="status">
              Updating your vault…
            </div>
          )}
          {showNotices && (
            <section className="panel notices">
              <div className="panel-heading">
                <h2>Notifications</h2>
                <button
                  className="subtle"
                  disabled={busy}
                  onClick={() => {
                    void act(
                      () => api("/notifications/refresh", "POST"),
                      "Reminders checked",
                    );
                  }}
                >
                  Check reminders
                </button>
              </div>
              {notices.length ? (
                notices.map((n) => (
                  <button
                    className={"notice " + (n.read ? "read" : "")}
                    key={n.id}
                    onClick={() => {
                      void act(
                        () => api(`/notifications/${n.id}/read`, "PATCH"),
                        "Marked as read",
                      );
                    }}
                  >
                    <Bell size={16} />
                    <span>{n.message}</span>
                    {!n.read && <span className="dot" />}
                  </button>
                ))
              ) : (
                <p className="muted pad">You're all caught up.</p>
              )}
            </section>
          )}
          {tab === "Dashboard" && (
            <>
              <div className="overview-caption">
                <span>
                  <span className="live-dot" /> YOUR OVERVIEW
                </span>
                <select
                  aria-label="Dashboard currency"
                  value={currency}
                  onChange={(e) => {
                    void act(
                      () =>
                        api("/settings", "PUT", {
                          ...settings,
                          currency: e.target.value,
                        }),
                      "Currency view updated",
                    );
                  }}
                >
                  {Array.from(
                    new Set([
                      currency,
                      ...(dashboard?.totals.map((t) => t.currency) || []),
                    ]),
                  ).map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div className="stat-grid">
                <Stat
                  label="Monthly commitment"
                  amount={money(totals?.monthly || 0, currency)}
                  icon={<Wallet size={19} />}
                  note="Normalized estimate"
                  featured
                />
                <Stat
                  label="Annual projection"
                  amount={money(totals?.annual || 0, currency)}
                  icon={<ChartNoAxesCombined size={19} />}
                  note="Based on active expenses"
                />
                <Stat
                  label="Active expenses"
                  amount={String(dashboard?.activeCount || 0).padStart(2, "0")}
                  icon={<Repeat size={19} />}
                  note={`${dashboard?.upcoming7.length || 0} due in the next 7 days`}
                />
                <Stat
                  label="Paid this month"
                  amount={money(totals?.paidThisMonth || 0, currency)}
                  icon={<ArrowDownLeft size={19} />}
                  note="Actual recorded payments"
                />
              </div>
              {!!dashboard?.overdue.length && (
                <div className="overdue-banner">
                  <CalendarDays size={20} />
                  <div>
                    <strong>
                      {dashboard.overdue.length}{" "}
                      {dashboard.overdue.length === 1
                        ? "expense needs"
                        : "expenses need"}{" "}
                      your attention
                    </strong>
                    <span>
                      Review overdue payments to keep your vault up to date.
                    </span>
                  </div>
                  <button
                    className="subtle"
                    onClick={() => changeTab("Upcoming")}
                  >
                    Review <ArrowUpRight size={16} />
                  </button>
                </div>
              )}
              <div className="dashboard-grid">
                <section className="panel upcoming-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>
                        Coming up next{" "}
                        <span className="count">
                          {dashboard?.upcoming30.length || 0}
                        </span>
                      </h2>
                      <p>Next payments within 30 days</p>
                    </div>
                    <button
                      className="text-button"
                      onClick={() => changeTab("Upcoming")}
                    >
                      View calendar <ArrowUpRight size={15} />
                    </button>
                  </div>
                  {rows(dashboard?.upcoming30.slice(0, 5) || [], true)}
                </section>
                <section className="panel spending-panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Where it goes</h2>
                      <p>Estimated monthly cost · {currency}</p>
                    </div>
                    <ChartNoAxesCombined size={19} />
                  </div>
                  {breakdown.length ? (
                    <>
                      <div
                        className="donut"
                        style={{
                          background: `conic-gradient(${breakdown
                            .map((c, i) => {
                              const sum = breakdown.reduce(
                                (a, b) => a + b.monthly,
                                0,
                              );
                              const start =
                                (breakdown
                                  .slice(0, i)
                                  .reduce((a, b) => a + b.monthly, 0) /
                                  sum) *
                                100;
                              return `${categories.find((k) => k.name === c.category)?.color || "#64748b"} ${start}% ${start + (c.monthly / sum) * 100}%`;
                            })
                            .join(",")})`,
                        }}
                      >
                        <div>
                          <small>Monthly total</small>
                          <strong>
                            {money(totals?.monthly || 0, currency)}
                          </strong>
                        </div>
                      </div>
                      <div className="legend">
                        {breakdown.map((c) => (
                          <div key={c.category}>
                            <span
                              className="category-dot"
                              style={{
                                background: categories.find(
                                  (k) => k.name === c.category,
                                )?.color,
                              }}
                            />
                            <span>{c.category}</span>
                            <strong>{money(c.monthly, c.currency)}</strong>
                          </div>
                        ))}
                      </div>
                    </>
                  ) : (
                    <Empty text="See your spending mix" />
                  )}
                </section>
              </div>
              <div className="dashboard-grid lower">
                <section className="panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Payment activity</h2>
                      <p>What you've actually paid over time</p>
                    </div>
                    <span className="tag">Last 6 months</span>
                  </div>
                  <TrendChart trends={trends} />
                </section>
                <section className="insight-card">
                  <div className="insight-icon">
                    <ShieldCheck size={23} />
                  </div>
                  <span className="eyebrow">A CLEARER PICTURE</span>
                  <h2>
                    Small expenses.
                    <br />
                    Real perspective.
                  </h2>
                  <p>
                    Your dashboard separates estimated commitments from the
                    payments you record.
                  </p>
                  <button
                    className="text-button"
                    onClick={() => changeTab("Reports")}
                  >
                    Explore your reports <ArrowUpRight size={16} />
                  </button>
                </section>
              </div>
            </>
          )}
          {(tab === "Subscriptions" || tab === "Bills") && (
            <section className="panel">
              <div className="filters">
                <div className="search">
                  <Search size={17} />
                  <input
                    aria-label="Search expenses"
                    placeholder="Search expenses…"
                    value={search}
                    onChange={(e) => {
                      setSearch(e.target.value);
                      setPage(0);
                    }}
                  />
                </div>
                <select
                  aria-label="Filter status"
                  value={status}
                  onChange={(e) => {
                    setStatus(e.target.value);
                    setPage(0);
                  }}
                >
                  <option value="">All statuses</option>
                  {["ACTIVE", "PAUSED", "CANCELED"].map((s) => (
                    <option value={s} key={s}>
                      {title(s)}
                    </option>
                  ))}
                </select>
                <select
                  aria-label="Filter category"
                  value={category}
                  onChange={(e) => {
                    setCategory(e.target.value);
                    setPage(0);
                  }}
                >
                  <option value="">All categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
                <select
                  aria-label="Sort expenses"
                  value={sort}
                  onChange={(e) => {
                    setSort(e.target.value);
                    setPage(0);
                  }}
                >
                  <option value="nextDueDate">Due date</option>
                  <option value="name">Name</option>
                  <option value="amount">Amount by currency</option>
                </select>
              </div>
              {rows(expenses.content)}
              <div className="pagination">
                <span>{expenses.totalElements} expenses</span>
                <div>
                  <button
                    className="icon-button"
                    aria-label="Previous page"
                    disabled={page === 0}
                    onClick={() => setPage(page - 1)}
                  >
                    <ChevronLeft size={18} />
                  </button>
                  <span>
                    Page {page + 1} of {Math.max(1, expenses.totalPages)}
                  </span>
                  <button
                    className="icon-button"
                    aria-label="Next page"
                    disabled={page + 1 >= expenses.totalPages}
                    onClick={() => setPage(page + 1)}
                  >
                    <ChevronRight size={18} />
                  </button>
                </div>
              </div>
            </section>
          )}

          {tab === "Upcoming" && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Payment calendar</h2>
                  <p>Estimated occurrences, including future repeats</p>
                </div>
                <input
                  aria-label="Calendar month"
                  type="month"
                  value={month}
                  onChange={(e) => {
                    if (e.target.value) setMonth(e.target.value);
                  }}
                />
              </div>
              {!!dashboard?.overdue.length && (
                <div className="pad">
                  <h3 className="overdue">Overdue now</h3>
                  {rows(dashboard.overdue, true)}
                </div>
              )}
              {calendar.length ? (
                <div className="calendar-list">
                  {calendar.map((item, i) => (
                    <div key={item.expenseId + "-" + i}>
                      <div className="calendar-date">
                        <strong>{item.date.slice(8)}</strong>
                        <small>
                          {new Date(item.date + "T12:00:00").toLocaleDateString(
                            "en-US",
                            { weekday: "short" },
                          )}
                        </small>
                      </div>
                      <strong>{item.name}</strong>
                      <span>{money(item.amount, item.currency)}</span>
                      <span className="tag">Projected</span>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty text="Nothing due this month" />
              )}
            </section>
          )}
          {tab === "Payment history" && (
            <section className="panel">
              <div className="panel-heading">
                <div>
                  <h2>Recorded payments</h2>
                  <p>Actual amounts, separate from your estimates</p>
                </div>
                <span className="tag">{payments.length} payments</span>
              </div>
              {payments.length ? (
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Expense</th>
                        <th>Occurrence</th>
                        <th>Paid on</th>
                        <th>Amount</th>
                        <th>Notes</th>
                      </tr>
                    </thead>
                    <tbody>
                      {payments.map((p) => (
                        <tr key={p.id}>
                          <td>
                            <strong>{p.expenseName}</strong>
                          </td>
                          <td>{dateLabel(p.occurrenceDate)}</td>
                          <td>{dateLabel(p.paymentDate)}</td>
                          <td>
                            <strong>{money(p.amount, p.currency)}</strong>
                          </td>
                          <td>{p.notes || "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <Empty text="Your payment story starts here" />
              )}
            </section>
          )}
          {tab === "Reports" && (
            <>
              <div className="report-controls">
                <Field label="Report month">
                  <input
                    type="month"
                    value={month}
                    onChange={(e) => {
                      if (e.target.value) setMonth(e.target.value);
                    }}
                  />
                </Field>
                <a
                  className="secondary"
                  href={`/api/reports/payments.csv?from=${month}-01&to=${month}-${new Date(Number(month.slice(0, 4)), Number(month.slice(5, 7)), 0).getDate()}`}
                >
                  <Download size={17} /> Export month CSV
                </a>
                <a
                  className="secondary"
                  href={`/api/reports/payments.csv?from=${month.slice(0, 4)}-01-01&to=${month.slice(0, 4)}-12-31`}
                >
                  <Download size={17} /> Export year CSV
                </a>
              </div>
              <div className="stat-grid report-stats">
                <Stat
                  label="Recorded in selected month"
                  amount={money(
                    reportTotals.find((r) => r.currency === currency)
                      ?.paidInMonth || 0,
                    currency,
                  )}
                  icon={<History size={19} />}
                  note={month}
                />
                <Stat
                  label="Recorded in selected year"
                  amount={money(
                    reportTotals.find((r) => r.currency === currency)
                      ?.paidInYear || 0,
                    currency,
                  )}
                  icon={<History size={19} />}
                  note={month.slice(0, 4)}
                />
                <Stat
                  label="Annual recurring estimate"
                  amount={money(totals?.annual || 0, currency)}
                  icon={<Repeat size={19} />}
                  note="Current active expenses, annualized"
                />
              </div>
              <section className="panel">
                <div className="panel-heading">
                  <h2>Actual payment trend · {currency}</h2>
                  <span className="tag">Last 6 months</span>
                </div>
                <TrendChart trends={trends} />
              </section>
              <section className="panel report-breakdown">
                <div className="panel-heading">
                  <h2>Monthly commitments by category</h2>
                  <span className="tag">{currency}</span>
                </div>
                {breakdown.length ? (
                  breakdown.map((c) => (
                    <div className="breakdown-row" key={c.category}>
                      <strong>{c.category}</strong>
                      <div className="bar-track">
                        <span
                          style={{
                            width: `${(c.monthly / Math.max(totals?.monthly || 1, 1)) * 100}%`,
                            background: categories.find(
                              (k) => k.name === c.category,
                            )?.color,
                          }}
                        />
                      </div>
                      <span>{money(c.monthly, c.currency)}</span>
                    </div>
                  ))
                ) : (
                  <Empty text="Add expenses to see a breakdown" />
                )}
              </section>
              <p className="muted">
                Currency view: {currency}. Change it on the dashboard or in
                settings. Currencies are reported separately; no exchange rates
                are applied.
              </p>
            </>
          )}
          {tab === "Settings" && (
            <div className="settings-grid">
              <section className="panel">
                <div className="panel-heading">
                  <div>
                    <h2>Workspace preferences</h2>
                    <p>Defaults for your personal vault</p>
                  </div>
                </div>
                <form
                  className="pad settings-form"
                  onSubmit={(e) => {
                    e.preventDefault();
                    void act(
                      () => api("/settings", "PUT", settings),
                      "Preferences saved",
                    );
                  }}
                >
                  <Field label="Default currency">
                    <select
                      value={settings.currency}
                      onChange={(e) =>
                        setSettings({ ...settings, currency: e.target.value })
                      }
                    >
                      {Array.from(
                        new Set([
                          "USD",
                          "EUR",
                          "GBP",
                          "CAD",
                          "INR",
                          "AUD",
                          currency,
                        ]),
                      ).map((c) => (
                        <option key={c}>{c}</option>
                      ))}
                    </select>
                  </Field>
                  <Field label="Appearance">
                    <select
                      value={settings.theme}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          theme: e.target.value as "light" | "dark",
                        })
                      }
                    >
                      <option value="light">Light</option>
                      <option value="dark">Dark</option>
                    </select>
                  </Field>
                  <Field label="Default reminder lead time (days)">
                    <input
                      type="number"
                      min="0"
                      max="90"
                      required
                      value={settings.reminderDays}
                      onChange={(e) =>
                        setSettings({
                          ...settings,
                          reminderDays: Number(e.target.value),
                        })
                      }
                    />
                  </Field>
                  <p className="muted">
                    Defaults apply to new expenses. Edit an expense to change
                    its existing reminder.
                  </p>
                  <button className="primary" disabled={busy}>
                    Save preferences
                  </button>
                </form>
              </section>
              <section className="panel">
                <div className="panel-heading">
                  <h2>Expense categories</h2>
                </div>
                <div className="pad">
                  {categories.map((c) => (
                    <div className="category-row" key={c.id}>
                      <span
                        className="category-dot"
                        style={{ background: c.color }}
                      />
                      {c.name}
                    </div>
                  ))}
                  <form
                    className="category-form"
                    onSubmit={(e) => {
                      e.preventDefault();
                      const form = e.currentTarget;
                      const data = new FormData(form);
                      void act(
                        () =>
                          api("/categories", "POST", {
                            name: data.get("name"),
                            color: data.get("color"),
                          }),
                        "Category added",
                      ).then((ok) => {
                        if (ok) form.reset();
                      });
                    }}
                  >
                    <input
                      name="name"
                      aria-label="Category name"
                      placeholder="New category"
                      maxLength={80}
                      required
                    />
                    <input
                      name="color"
                      aria-label="Category color"
                      type="color"
                      defaultValue="#6366f1"
                    />
                    <button className="secondary" disabled={busy}>
                      <Plus size={16} /> Add
                    </button>
                  </form>
                </div>
              </section>
            </div>
          )}
          <footer>
            <span className="logo-text">BillVault</span>
            <span>A little more clarity. Every month.</span>
            <span>
              <ShieldCheck size={13} /> Self-hosted & private
            </span>
          </footer>
        </main>
      </div>
      {toast && (
        <div className="toast" role="status">
          <Check size={18} />
          {toast}
        </div>
      )}
      {editing && (
        <Modal
          title={editId ? "Edit expense" : "Add a recurring expense"}
          onClose={() => {
            if (!busy) setEditing(null);
          }}
        >
          <ExpenseForm
            key={editId || "new"}
            initial={editing}
            categories={categories}
            busy={busy}
            error={error}
            onCancel={() => setEditing(null)}
            onSave={(value) => {
              void act(
                () =>
                  api(
                    "/expenses" + (editId ? "/" + editId : ""),
                    editId ? "PUT" : "POST",
                    value,
                  ),
                "Expense saved",
              ).then((ok) => {
                if (ok) setEditing(null);
              });
            }}
            onDelete={editId ? () => setDeleting(editId) : undefined}
            onReminder={
              editId
                ? () => {
                    void api<{ daysBefore: number; enabled: boolean }>(
                      `/expenses/${editId}/reminder`,
                    )
                      .then((r) => setReminder({ id: editId, ...r }))
                      .catch((e) => setError(e.message));
                  }
                : undefined
            }
          />
        </Modal>
      )}
      {paying && (
        <Modal
          title={"Record payment · " + paying.name}
          onClose={() => {
            if (!busy) setPaying(null);
          }}
        >
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void act(
                () =>
                  api(`/expenses/${paying.id}/payments`, "POST", {
                    occurrenceDate: paying.nextDueDate,
                    paymentDate: payDate,
                    amount: Number(payAmount),
                    notes: payNotes,
                  }),
                "Payment recorded. Next due date updated.",
              ).then((ok) => {
                if (ok) setPaying(null);
              });
            }}
          >
            <p className="muted">
              For the occurrence due {dateLabel(paying.nextDueDate)}. This
              records a payment; it does not move money.
            </p>
            <div className="form-grid">
              <Field label={"Amount paid (" + paying.currency + ")"}>
                <input
                  type="number"
                  min="0.01"
                  max="9999999999.99"
                  step="0.01"
                  required
                  value={payAmount}
                  onChange={(e) => setPayAmount(e.target.value)}
                />
              </Field>
              <Field label="Payment date">
                <input
                  type="date"
                  max={today()}
                  required
                  value={payDate}
                  onChange={(e) => setPayDate(e.target.value)}
                />
              </Field>
              <Field label="Notes" wide>
                <textarea
                  maxLength={2000}
                  value={payNotes}
                  onChange={(e) => setPayNotes(e.target.value)}
                />
              </Field>
            </div>
            {error && (
              <div role="alert" className="error">
                {error}
              </div>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="secondary"
                disabled={busy}
                onClick={() => setPaying(null)}
              >
                Cancel
              </button>
              <button className="primary" disabled={busy}>
                Record payment
              </button>
            </div>
          </form>
        </Modal>
      )}
      {deleting && (
        <Modal
          title="Delete this expense?"
          onClose={() => {
            if (!busy) setDeleting(null);
          }}
        >
          <p>
            This permanently deletes this expense and its payment history and
            reminders. To keep your records, change its status to canceled
            instead.
          </p>
          <div className="modal-actions">
            <button
              className="secondary"
              disabled={busy}
              onClick={() => setDeleting(null)}
            >
              Keep expense
            </button>
            <button
              className="danger"
              disabled={busy}
              onClick={() => {
                void act(
                  () => api("/expenses/" + deleting, "DELETE"),
                  "Expense deleted",
                ).then((ok) => {
                  if (ok) {
                    setDeleting(null);
                    setEditing(null);
                  }
                });
              }}
            >
              Delete permanently
            </button>
          </div>
        </Modal>
      )}
      {reminder && (
        <Modal
          title="Expense reminder"
          onClose={() => {
            if (!busy) setReminder(null);
          }}
        >
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void act(
                () => api(`/expenses/${reminder.id}/reminder`, "PUT", reminder),
                "Reminder updated",
              ).then((ok) => {
                if (ok) setReminder(null);
              });
            }}
          >
            <Field label="Days before payment">
              <input
                type="number"
                required
                min="0"
                max="90"
                value={reminder.daysBefore}
                onChange={(e) =>
                  setReminder({
                    ...reminder,
                    daysBefore: Number(e.target.value),
                  })
                }
              />
            </Field>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={reminder.enabled}
                onChange={(e) =>
                  setReminder({ ...reminder, enabled: e.target.checked })
                }
              />{" "}
              Enable in-app reminders
            </label>
            <div className="modal-actions">
              <button className="primary" disabled={busy}>
                Save reminder
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
