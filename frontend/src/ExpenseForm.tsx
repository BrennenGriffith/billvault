import { useState, type FormEvent } from "react";
import type { Category, ExpenseInput } from "./types";
import { Field } from "./components";
import { title } from "./format";
export default function ExpenseForm({
  initial,
  categories,
  busy,
  onSave,
  onCancel,
  onDelete,
  onReminder,
  error,
}: {
  initial: ExpenseInput;
  categories: Category[];
  busy: boolean;
  onSave: (e: ExpenseInput) => void;
  onCancel: () => void;
  onDelete?: () => void;
  onReminder?: () => void;
  error: string;
}) {
  const [value, setValue] = useState(initial);
  const set = (patch: Partial<ExpenseInput>) =>
    setValue({ ...value, ...patch });
  function submit(e: FormEvent) {
    e.preventDefault();
    onSave(value);
  }
  return (
    <form onSubmit={submit}>
      <p className="muted">One place for the things you pay for regularly.</p>
      <div className="form-grid">
        <Field label="Name">
          <input
            required
            maxLength={120}
            value={value.name}
            onChange={(e) => set({ name: e.target.value })}
          />
        </Field>
        <Field label="Type">
          <select
            value={value.kind}
            onChange={(e) =>
              set({ kind: e.target.value as ExpenseInput["kind"] })
            }
          >
            <option value="SUBSCRIPTION">Subscription</option>
            <option value="BILL">Household bill</option>
          </select>
        </Field>
        <Field label="Category">
          <select
            value={value.categoryId}
            required
            onChange={(e) => set({ categoryId: Number(e.target.value) })}
          >
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Status">
          <select
            value={value.status}
            onChange={(e) =>
              set({ status: e.target.value as ExpenseInput["status"] })
            }
          >
            {["ACTIVE", "PAUSED", "CANCELED"].map((s) => (
              <option value={s} key={s}>
                {title(s)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Estimated amount">
          <input
            type="number"
            min="0.01"
            max="9999999999.99"
            step="0.01"
            required
            value={value.amount || ""}
            onChange={(e) => set({ amount: Number(e.target.value) })}
          />
        </Field>
        <Field label="Currency (ISO code)">
          <input
            required
            pattern="[A-Z]{3}"
            maxLength={3}
            value={value.currency}
            onChange={(e) => set({ currency: e.target.value.toUpperCase() })}
          />
        </Field>
        <Field label="Billing frequency">
          <select
            value={value.frequency}
            onChange={(e) =>
              set({
                frequency: e.target.value as ExpenseInput["frequency"],
                customInterval: e.target.value === "CUSTOM" ? 1 : null,
                customUnit: e.target.value === "CUSTOM" ? "MONTHS" : null,
              })
            }
          >
            {["WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM"].map((f) => (
              <option key={f}>{f}</option>
            ))}
          </select>
        </Field>
        <Field label="Payment method label">
          <input
            maxLength={120}
            placeholder="e.g. Personal card"
            value={value.paymentMethod || ""}
            onChange={(e) => set({ paymentMethod: e.target.value })}
          />
        </Field>
        {value.frequency === "CUSTOM" && (
          <>
            <Field label="Repeat every">
              <input
                type="number"
                min="1"
                max="3650"
                required
                value={value.customInterval || 1}
                onChange={(e) =>
                  set({ customInterval: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Interval unit">
              <select
                value={value.customUnit || "MONTHS"}
                onChange={(e) =>
                  set({
                    customUnit: e.target.value as ExpenseInput["customUnit"],
                  })
                }
              >
                {["DAYS", "WEEKS", "MONTHS", "YEARS"].map((u) => (
                  <option key={u}>{u}</option>
                ))}
              </select>
            </Field>
          </>
        )}
        <Field label="Start date (billing anchor)">
          <input
            type="date"
            required
            value={value.startDate}
            onChange={(e) => set({ startDate: e.target.value })}
          />
        </Field>
        <Field label="Next payment date">
          <input
            type="date"
            min={value.startDate}
            required
            value={value.nextDueDate}
            onChange={(e) => set({ nextDueDate: e.target.value })}
          />
        </Field>
        <Field label="End date (optional)">
          <input
            type="date"
            min={value.nextDueDate}
            value={value.endDate || ""}
            onChange={(e) => set({ endDate: e.target.value || null })}
          />
        </Field>
        <Field label="Website (optional)">
          <input
            type="url"
            maxLength={500}
            placeholder="https://"
            value={value.websiteUrl || ""}
            onChange={(e) => set({ websiteUrl: e.target.value })}
          />
        </Field>
        <Field label="Description" wide>
          <input
            maxLength={1000}
            value={value.description || ""}
            onChange={(e) => set({ description: e.target.value })}
          />
        </Field>
        <Field label="Notes" wide>
          <textarea
            maxLength={2000}
            value={value.notes || ""}
            onChange={(e) => set({ notes: e.target.value })}
          />
        </Field>
        <label className="checkbox wide">
          <input
            type="checkbox"
            checked={value.variableAmount}
            onChange={(e) => set({ variableAmount: e.target.checked })}
          />{" "}
          Actual payment amount may vary
        </label>
      </div>
      {error && (
        <div className="error" role="alert">
          {error}
        </div>
      )}
      <div className="modal-actions">
        {onDelete && (
          <button type="button" className="danger-text" onClick={onDelete}>
            Delete
          </button>
        )}
        {onReminder && (
          <button type="button" className="subtle" onClick={onReminder}>
            Reminder
          </button>
        )}
        <span className="spacer" />
        <button
          type="button"
          className="secondary"
          disabled={busy}
          onClick={onCancel}
        >
          Cancel
        </button>
        <button className="primary" disabled={busy}>
          {busy ? "Saving…" : "Save expense"}
        </button>
      </div>
    </form>
  );
}
