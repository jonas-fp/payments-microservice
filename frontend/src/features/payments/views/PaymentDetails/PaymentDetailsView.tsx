import { useEffect, useState, type SubmitEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Loader } from '../../../../components/Loader';
import { usePaymentDetails } from '../../hooks/usePaymentDetails';
import { PaymentDetailsTable } from '../../components/PaymentDetailsTable';
import { PaymentHistoryTable } from '../../components/PaymentHistoryTable';

import './PaymentDetailsView.css';

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function PaymentDetailsView() {
  const { paymentId } = useParams<{ paymentId: string }>();
  const navigate = useNavigate();
  const [inputUuid, setInputUuid] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [refreshSignal, setRefreshSignal] = useState<boolean>(false);

  useEffect(() => {
    setInputUuid(paymentId ?? '');
    setFormError(null);
  }, [paymentId]);

  const { paymentData, isLoading, error } = usePaymentDetails(
    paymentId,
    refreshSignal,
  );

  const handleSubmit = (e: SubmitEvent) => {
    e.preventDefault();
    const trimmed = inputUuid.trim();

    if (!trimmed) {
      setFormError('Enter a payment ID.');
      return;
    }

    if (!UUID_PATTERN.test(trimmed)) {
      setFormError(
        `"${trimmed}" doesn't look like a valid payment ID (expected a UUID).`,
      );
      return;
    }

    setFormError(null);
    void navigate(`/payments/payment-details/${trimmed}`);
  };

  return (
    <div>
      <h2>Payment Details</h2>

      <div className='payment-details-options'>
        <form onSubmit={handleSubmit} noValidate>
          <input
            type='text'
            placeholder='Enter payment UUID...'
            value={inputUuid}
            onChange={(e) => {
              setInputUuid(e.target.value);
              if (formError) setFormError(null);
            }}
            aria-invalid={formError ? true : undefined}
          />
          <button className='lookup-button' type='submit'>
            Look up
          </button>
        </form>

        <button
          className='refresh-button'
          onClick={() => setRefreshSignal((prev) => !prev)}
          disabled={!paymentId || isLoading}
        >
          Refresh
        </button>
      </div>

      {formError && <p className='payment-details-error'>{formError}</p>}

      {isLoading && <Loader />}

      {!isLoading && error && <p className='payment-details-error'>{error}</p>}

      {!isLoading && !error && !paymentId && (
        <p>Enter a payment UUID above to look up its details.</p>
      )}

      {!isLoading && !error && paymentData && (
        <>
          <PaymentDetailsTable paymentData={paymentData} />
          <h3>History</h3>
          <PaymentHistoryTable history={paymentData.history} />
        </>
      )}
    </div>
  );
}
