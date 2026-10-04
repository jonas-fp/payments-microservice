import { useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import { reconciliationService } from '../services/reconciliationService';
import type { ReconciliationRunSummary } from '../types/reconciliation';

export function useReconciliationRun(
  runId: string | undefined,
  refreshSignal: boolean,
) {
  const requestKey = runId ? `${runId}:${refreshSignal}` : null;
  const [result, setResult] = useState<{
    requestKey: string;
    runData: ReconciliationRunSummary | null;
    error: string | null;
  } | null>(null);

  useEffect(() => {
    if (!runId || !requestKey) return;

    reconciliationService
      .getRun(runId)
      .then((runData) => {
        setResult({ requestKey, runData, error: null });
      })
      .catch((err: unknown) => {
        console.error('Error fetching reconciliation run: ', err);
        setResult({
          requestKey,
          runData: null,
          error: toFriendlyErrorMessage(err, runId),
        });
      });
  }, [requestKey, runId]);

  if (!requestKey) {
    return { runData: null, isLoading: false, error: null };
  }

  if (result?.requestKey !== requestKey) {
    return { runData: null, isLoading: true, error: null };
  }

  return {
    runData: result.runData,
    isLoading: false,
    error: result.error,
  };
}

function toFriendlyErrorMessage(err: unknown, runId: string): string {
  if (isAxiosError(err) && err.response?.status === 404) {
    return `No reconciliation run was found with ID "${runId}".`;
  }

  return 'Something went wrong while loading this reconciliation run. Please try again.';
}
