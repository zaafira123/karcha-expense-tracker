export type ApiError = { error: string; message: string; fields?: Record<string, string> }
export const CATEGORIES = [
  { code: 'FOOD_DINING', name: 'Food and dining' }, { code: 'GROCERIES', name: 'Groceries' },
  { code: 'TRAVEL', name: 'Travel' }, { code: 'SHOPPING', name: 'Shopping' },
  { code: 'BILLS_UTILITIES', name: 'Bills and utilities' }, { code: 'HEALTH', name: 'Health' },
  { code: 'ENTERTAINMENT', name: 'Entertainment' }, { code: 'SAVINGS', name: 'Savings' },
] as const
export type AuthResponse = { accessToken: string; tokenType: string; expiresIn: number }
export type UserProfile = { id: string; email: string; createdAt: string; agentCapPaise: number | null }
export type SettingsData = { email: string; agentCapPaise: number }
export type WeeklySummary = { currentWeekStart: string; currentWeekEnd: string; currentWeekSpentPaise: number; previousWeekSpentPaise: number; changePaise: number; categoryIncreases: { category: string; categoryName: string; changePaise: number }[]; topCategories: { category: string; categoryName: string; amountPaise: number }[]; budgetsOver80: { category: string; categoryName: string; spentPaise: number; budgetPaise: number; percentUsed: number }[]; tips: string[]; summaryText: string }
export type MonthlySummary = { month: string; currentSpentPaise: number; previousSpentPaise: number; changePaise: number; topCategories: { category: string; categoryName: string; amountPaise: number }[]; tips: string[]; summaryText: string }
export type CategoryInfo = { code: string; displayName: string; isProtected: boolean }
export type CategoryProgress = { category: CategoryInfo; budgetPaise: number | null; spentPaise: number | null; percentUsed: number; status: 'OK' | 'WARNING' | 'OVER' }
export type Transaction = { id: string; amountPaise: number; category: CategoryInfo; merchant: string; occurredOn: string; note: string | null; source: string; createdAt: string }
export type DashboardData = { totalBudgetPaise: number | null; spentPaise: number | null; remainingPaise: number | null; daysLeft: number; categories: CategoryProgress[]; recent: Transaction[] }
export type ParseExpenseDraft = { amountPaise: number | null; category: string | null; categoryName: string | null; merchant: string | null; occurredOn: string | null; note: string | null; source: 'TEXT'; confidence: string; needsReview: boolean; method: string }
export type TransactionInput = { amountPaise: number; category: string; merchant: string; occurredOn: string; note: string | null; source: 'TEXT' | 'MANUAL' }
export type SmsDraftStatus = 'READY' | 'NEEDS_REVIEW' | 'DUPLICATE' | 'CREDIT' | 'UNKNOWN'
export type SmsImportDraft = { index: number; type: 'DEBIT' | 'CREDIT' | 'UNKNOWN'; amountPaise: number | null; merchant: string | null; occurredOn: string | null; category: string | null; categoryName: string | null; status: SmsDraftStatus; reason: string | null; messageHash: string | null; referenceHint: string | null; confidence: string }
export type SmsParseResponse = { drafts: SmsImportDraft[] }
export type SmsSaveRequest = { consentAccepted: true; drafts: { index: number; amountPaise: number; category: string; merchant: string; occurredOn: string; messageHash: string; type: 'DEBIT' }[] }
export type SmsSaveResponse = { savedCount: number; skipped: { index: number; reason: string }[] }
export type BudgetEntry = { category: CategoryInfo; amountPaise: number; spentPaise: number }
export type ForecastPoint = { day: number; actualPaise: number | null; projectedPaise: number | null; budgetPaise: number }
export type CategoryForecast = { category: string; categoryName: string; projectedPaise: number | null; budgetPaise: number; overByPaise: number }
export type ForecastData = { month: string; status: 'OK' | 'OVER_BUDGET_PROJECTED' | 'INSUFFICIENT_DATA'; spentPaise: number; projectedPaise: number | null; lowPaise: number | null; highPaise: number | null; budgetPaise: number; avgDailyPaise: number | null; daysLeft: number; series: ForecastPoint[]; categories: CategoryForecast[] }
export type AgentProposal = { id: string; month: string; fromCategory: string; fromCategoryName: string; toCategory: string; toCategoryName: string; amountPaise: number; shortfallPaise: number; donorSparePaise: number; status: 'PENDING' | 'APPROVED' | 'APPLIED' | 'UNDONE' | 'REJECTED' | 'EXPIRED'; createdAt: string; decidedAt: string | null; appliedAt: string | null; expiresAt: string }
export type AgentAudit = { id: string; proposalId: string | null; event: string; detail: string; createdAt: string }
export type AgentWhy = { proposalId: string; explanation: string }
export type UndoResponse = { proposal: AgentProposal; budgetsChangedSinceApply: boolean }

export function userMessage(error: ApiError | null | undefined, fallback = 'Something went wrong. Please try again.') {
  return error?.message || fallback
}
