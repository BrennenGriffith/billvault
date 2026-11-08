import { useEffect, useRef, type ReactNode } from "react";
import {
  Plus,
  Layers,
  Check,
  MoreHorizontal,
  X,
  ChartNoAxesCombined,
} from "lucide-react";
import { money, dateLabel, title, today } from "./format";
import type { Expense } from "./types";
export function Field({
  label,
  children,
  wide = false,
}: {
  label: string;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <label className={wide ? "field wide" : "field"}>
      <span>{label}</span>
      {children}
    </label>
  );
}
export function Empty({ text, action }: { text: string; action?: () => void }) {
  return (
    <div className="empty">
      <div className="empty-icon">
        <Layers size={28} />
      </div>
      <h3>{text}</h3>
      <p>Your recurring expenses will come together here.</p>
      {action && (
        <button className="primary" onClick={action}>
          <Plus size={16} /> Add your first expense
        </button>
      )}
    </div>
  );
}
export function ExpenseTable({
  items,
  compact = false,
  onAdd,
  onEdit,
  onPay,
}: {
  items: Expense[];
  compact?: boolean;
  onAdd: () => void;
  onEdit: (e: Expense) => void;
  onPay: (e: Expense) => void;
}) {
  return items.length ? (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Expense</th>
            {!compact && <th>Category</th>}
            <th>Amount</th>
            <th>Next payment</th>
            {!compact && <th>Status</th>}
            <th>
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {items.map((e) => (
            <tr key={e.id}>
              <td>
                <div className="expense-name">
                  <span
                    className="brand-tile"
                    style={{
                      background: e.categoryColor + "18",
                      color: e.categoryColor,
                    }}
                  >
                    {e.name.slice(0, 2).toUpperCase()}
                  </span>
                  <div>
                    <strong>{e.name}</strong>
                    <small>
                      {title(e.frequency)}
                      {e.variableAmount ? " · Variable" : ""}
                    </small>
                  </div>
                </div>
              </td>
              {!compact && (
                <td>
                  <span
                    className="category-dot"
                    style={{ background: e.categoryColor }}
                  />
                  {e.categoryName}
                </td>
              )}
              <td>
                <strong>{money(e.amount, e.currency)}</strong>
              </td>
              <td
                className={
                  e.nextDueDate < today() && e.status === "ACTIVE"
                    ? "overdue"
                    : ""
                }
              >
                {dateLabel(e.nextDueDate)}
              </td>
              {!compact && (
                <td>
                  <span className={"badge " + e.status.toLowerCase()}>
                    {title(e.status)}
                  </span>
                </td>
              )}
              <td>
                <div className="row-actions">
                  {e.status === "ACTIVE" && (
                    <button
                      className="subtle"
                      onClick={() => onPay(e)}
                      title="Record payment"
                    >
                      <Check size={16} />
                    </button>
                  )}
                  <button
                    className="icon-button"
                    title={"Edit " + e.name}
                    onClick={() => onEdit(e)}
                  >
                    <MoreHorizontal size={19} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ) : (
    <Empty text="A fresh start for your finances" action={onAdd} />
  );
}
export function Stat({
  label,
  amount,
  icon,
  note,
  featured = false,
}: {
  label: string;
  amount: string;
  icon: ReactNode;
  note: string;
  featured?: boolean;
}) {
  return (
    <section className={"stat " + (featured ? "featured" : "")}>
      <div className="stat-top">
        <span>{label}</span>
        <span className="stat-icon">{icon}</span>
      </div>
      <strong className="stat-value">{amount}</strong>
      <small>
        <span className="tiny-dot" />
        {note}
      </small>
    </section>
  );
}
export function TrendChart({
  trends,
}: {
  trends: { month: string; amount: number; currency: string }[];
}) {
  const max = Math.max(...trends.map((t) => t.amount), 1);
  return (
    <div
      className="trend-chart"
      role="img"
      aria-label="Actual payments by month"
    >
      {trends.length ? (
        trends.map((t) => (
          <div className="trend-column" key={t.month}>
            <span>{money(t.amount, t.currency)}</span>
            <div className="trend-bar-area">
              <div
                className="trend-bar"
                style={{ height: Math.max((t.amount / max) * 100, 2) + "%" }}
              />
            </div>
            <small>
              {new Date(t.month + "-15T12:00:00").toLocaleDateString("en-US", {
                month: "short",
              })}
            </small>
          </div>
        ))
      ) : (
        <div className="empty-chart">
          <ChartNoAxesCombined size={30} />
          <p>Record a payment to start your spending story.</p>
        </div>
      )}
    </div>
  );
}
export function Modal({
  title: heading,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const dialog = document.querySelector<HTMLElement>(
      ".overlay:last-of-type .modal",
    );
    dialog?.focus();
    const handler = (e: KeyboardEvent) => {
      if (dialog !== document.querySelector(".overlay:last-of-type .modal"))
        return;
      if (e.key === "Escape") closeRef.current();
      if (e.key === "Tab") {
        const nodes = dialog?.querySelectorAll<HTMLElement>(
          "button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled)",
        );
        if (nodes?.length) {
          const first = nodes[0],
            last = nodes[nodes.length - 1];
          if (
            e.shiftKey &&
            (document.activeElement === first ||
              document.activeElement === dialog)
          ) {
            e.preventDefault();
            last.focus();
          } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    };
    document.addEventListener("keydown", handler);
    return () => {
      document.removeEventListener("keydown", handler);
      previous?.focus();
    };
  }, []);
  return (
    <div
      className="overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section
        className="modal"
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-label={heading}
      >
        <div className="modal-heading">
          <h2>{heading}</h2>
          <button
            className="icon-button"
            aria-label="Close dialog"
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}
