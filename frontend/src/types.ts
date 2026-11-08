export type Kind = "SUBSCRIPTION" | "BILL";
export type Status = "ACTIVE" | "PAUSED" | "CANCELED";
export type Frequency =
  "WEEKLY" | "MONTHLY" | "QUARTERLY" | "YEARLY" | "CUSTOM";
export interface Category {
  id: number;
  name: string;
  color: string;
}
export interface Expense {
  id: number;
  name: string;
  description: string | null;
  kind: Kind;
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  amount: number;
  currency: string;
  frequency: Frequency;
  customInterval: number | null;
  customUnit: "DAYS" | "WEEKS" | "MONTHS" | "YEARS" | null;
  startDate: string;
  nextDueDate: string;
  endDate: string | null;
  paymentMethod: string | null;
  websiteUrl: string | null;
  status: Status;
  variableAmount: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}
export type ExpenseInput = Omit<
  Expense,
  | "id"
  | "categoryName"
  | "categoryColor"
  | "version"
  | "createdAt"
  | "updatedAt"
> & { version?: number };
export interface Payment {
  id: number;
  expenseId: number;
  expenseName: string;
  occurrenceDate: string;
  paymentDate: string;
  amount: number;
  currency: string;
  notes: string | null;
}
export interface Settings {
  currency: string;
  theme: "light" | "dark";
  reminderDays: number;
}
export interface Notice {
  id: number;
  expenseId: number;
  message: string;
  occurrenceDate: string;
  read: boolean;
  createdAt: string;
}
export interface Dashboard {
  activeCount: number;
  totals: {
    currency: string;
    monthly: number;
    annual: number;
    paidThisMonth: number;
  }[];
  upcoming7: Expense[];
  upcoming30: Expense[];
  overdue: Expense[];
  categories: { category: string; currency: string; monthly: number }[];
  mostExpensive: Expense[];
  recentPayments: Payment[];
  trends: { month: string; currency: string; amount: number }[];
}
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface CalendarItem {
  expenseId: number;
  name: string;
  date: string;
  amount: number;
  currency: string;
}

export interface ReportTotal {
  currency: string;
  paidInMonth: number;
  paidInYear: number;
}
