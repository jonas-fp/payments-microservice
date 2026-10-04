import { axiosInstance } from '../../../services/axiosInstance';
import type { ReconciliationRunSummary } from '../types/reconciliation';

export const reconciliationService = {
  getRun: async (runId: string): Promise<ReconciliationRunSummary> => {
    const response = await axiosInstance.get<ReconciliationRunSummary>(
      `/v1/payments/reconciliation/runs/${runId}`,
    );
    return response.data;
  },
};
