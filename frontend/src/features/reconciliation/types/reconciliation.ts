export type ReconciliationRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED';

export type ReconciliationBreakType =
  | 'MISSING_INTERNAL_RECORD'
  | 'MISSING_PROCESSOR_RECORD'
  | 'DUPLICATE_INTERNAL_RECORD'
  | 'DUPLICATE_PROCESSOR_RECORD'
  | 'AMOUNT_MISMATCH';

export interface ReconciliationRunSummary {
  id: string;
  businessDate: string;
  status: ReconciliationRunStatus;
  startedAt: string | null;
  completedAt: string | null;
  breakSummary: Partial<Record<ReconciliationBreakType, number>>;
}
