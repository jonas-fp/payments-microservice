import { useEffect, useState } from 'react';
import type { TrialBalanceResponse } from '../types/ledger';
import { ledgerService } from '../services/ledgerService';

export function useTrialBalance() {
  const [trialBalanceData, setTrialBalanceData] =
    useState<TrialBalanceResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    ledgerService
      .getTrialBalance()
      .then(setTrialBalanceData)
      .catch((err) => {
        console.error('Error fetching trial balance: ', err);
        setError(`Failed to load trial balance data: ${err.message}`);
      })
      .finally(() => setIsLoading(false));
  }, []);

  return { trialBalanceData, isLoading, error };
}
