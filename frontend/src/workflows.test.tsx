// @vitest-environment jsdom
import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import ExpenseForm from "./ExpenseForm";
import { ExpenseTable } from "./components";
import type { ExpenseInput } from "./types";
import { money, today } from "./format";
const initial: ExpenseInput = {
  name: "Internet",
  description: "",
  kind: "BILL",
  categoryId: 1,
  amount: 79.99,
  currency: "USD",
  frequency: "MONTHLY",
  customInterval: null,
  customUnit: null,
  startDate: "2026-01-31",
  nextDueDate: "2026-01-31",
  endDate: null,
  paymentMethod: "Card",
  websiteUrl: "",
  status: "ACTIVE",
  variableAmount: true,
  notes: "",
};
afterEach(cleanup);
describe("Expense workflows", () => {
  it("submits edited amounts and custom intervals", () => {
    const save = vi.fn();
    render(
      <ExpenseForm
        initial={initial}
        categories={[{ id: 1, name: "Utilities", color: "#123456" }]}
        busy={false}
        error=""
        onSave={save}
        onCancel={() => {}}
      />,
    );
    fireEvent.change(screen.getByLabelText("Estimated amount"), {
      target: { value: "84.25" },
    });
    fireEvent.change(screen.getByLabelText("Billing frequency"), {
      target: { value: "CUSTOM" },
    });
    fireEvent.change(screen.getByLabelText("Repeat every"), {
      target: { value: "2" },
    });
    fireEvent.click(screen.getByText("Save expense"));
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        amount: 84.25,
        frequency: "CUSTOM",
        customInterval: 2,
        customUnit: "MONTHS",
      }),
    );
  });
  it("offers a clear empty-state action", () => {
    const add = vi.fn();
    render(
      <ExpenseTable
        items={[]}
        onAdd={add}
        onEdit={() => {}}
        onPay={() => {}}
      />,
    );
    fireEvent.click(screen.getByText("Add your first expense"));
    expect(add).toHaveBeenCalledOnce();
  });
  it("shows server validation errors", () => {
    render(
      <ExpenseForm
        initial={initial}
        categories={[]}
        busy={false}
        error="Unknown currency"
        onSave={() => {}}
        onCancel={() => {}}
      />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Unknown currency");
  });
  it("formats currencies separately and produces local ISO dates", () => {
    expect(money(79.99, "USD")).toBe("$79.99");
    expect(money(79.99, "EUR")).toContain("79.99");
    expect(today()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
