export interface TrialBalanceResponse {
  asOf: string;
  totalDebits: number;
  totalCredits: number;
  isBalanced: boolean;
  entries: TrialBalanceEntry[];
}

export interface TrialBalanceEntry {
  accountCode: string;
  accountName: string;
  totalDebit: number;
  totalCredit: number;
}
