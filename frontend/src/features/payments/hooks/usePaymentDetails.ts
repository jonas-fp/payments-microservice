import { useEffect, useState } from 'react';
import { isAxiosError } from 'axios';
import type { PaymentDetailsResponse } from '../types/payments';
import { paymentService } from '../services/paymentService';

export function usePaymentDetails(
  paymentId: string | undefined,
  refreshSignal: boolean,
) {
  const [paymentData, setPaymentData] = useState<PaymentDetailsResponse | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!paymentId) {
      setPaymentData(null);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    setPaymentData(null);

    paymentService
      .getPaymentDetails(paymentId)
      .then(setPaymentData)
      .catch((err) => {
        console.error('Error fetching payment details: ', err);
        setError(toFriendlyErrorMessage(err, paymentId));
      })
      .finally(() => setIsLoading(false));
  }, [paymentId, refreshSignal]);

  return { paymentData, isLoading, error };
}

function toFriendlyErrorMessage(err: unknown, paymentId: string): string {
  if (isAxiosError(err)) {
    if (err.response?.status === 404) {
      return `No payment was found with ID "${paymentId}".`;
    }

    if (err.response?.status === 400) {
      return `"${paymentId}" is not a valid payment ID. Payment IDs must be a valid UUID.`;
    }
  }

  return 'Something went wrong while loading this payment. Please try again.';
}
