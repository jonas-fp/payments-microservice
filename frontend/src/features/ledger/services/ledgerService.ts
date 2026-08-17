import { axiosInstance } from '../../../services/axiosInstance';
import type { TrialBalanceResponse } from '../types/ledger';

export const ledgerService = {
  getTrialBalance: async (asOf?: string): Promise<TrialBalanceResponse> => {
    const params = asOf ? { asOf } : {};
    const response = await axiosInstance.get<TrialBalanceResponse>(
      '/v1/payments/subledger/trial-balance',
      { params },
    );
    return response.data;
  },
};
