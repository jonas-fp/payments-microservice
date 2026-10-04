import { useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Loader } from '../../../../components/Loader';
import { ReconciliationBreaksTable } from '../../components/ReconciliationBreaksTable';
import { useReconciliationRun } from '../../hooks/useReconciliationRun';

import './ReconciliationBreaksView.css';

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function ReconciliationBreaksView() {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  const [editedInput, setEditedInput] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [refreshSignal, setRefreshSignal] = useState(false);
  const inputUuid = editedInput ?? runId ?? '';

  const { runData, isLoading, error } = useReconciliationRun(
    runId,
    refreshSignal,
  );

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = inputUuid.trim();

    if (!trimmed) {
      setFormError('Enter a reconciliation run ID.');
      return;
    }

    if (!UUID_PATTERN.test(trimmed)) {
      setFormError(
        `"${trimmed}" doesn't look like a valid reconciliation run ID (expected a UUID).`,
      );
      return;
    }

    setFormError(null);
    setEditedInput(null);
    void navigate(`/reconciliation/breaks/${trimmed}`);
  };

  return (
    <div className='reconciliation-breaks-view'>
      <h2>Reconciliation Breaks</h2>

      <div className='reconciliation-breaks-options'>
        <form onSubmit={handleSubmit} noValidate>
          <input
            type='text'
            aria-label='Reconciliation run ID'
            placeholder='Enter reconciliation run UUID...'
            value={inputUuid}
            onChange={(event) => {
              setEditedInput(event.target.value);
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
          onClick={() => setRefreshSignal((previous) => !previous)}
          disabled={!runId || isLoading}
        >
          Refresh
        </button>
      </div>

      {formError && (
        <p className='reconciliation-breaks-error'>{formError}</p>
      )}

      {isLoading && <Loader />}

      {!isLoading && error && (
        <p className='reconciliation-breaks-error'>{error}</p>
      )}

      {!isLoading && !error && !runId && (
        <p>Enter a reconciliation run UUID above to view its breaks.</p>
      )}

      {!isLoading && !error && runData && (
        <>
          <dl className='reconciliation-run-details'>
            <div>
              <dt>Business Date</dt>
              <dd>{runData.businessDate}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{formatLabel(runData.status)}</dd>
            </div>
            <div>
              <dt>Started</dt>
              <dd>{formatDateTime(runData.startedAt)}</dd>
            </div>
            <div>
              <dt>Completed</dt>
              <dd>{formatDateTime(runData.completedAt)}</dd>
            </div>
          </dl>
          <ReconciliationBreaksTable runData={runData} />
        </>
      )}
    </div>
  );
}

function formatLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function formatDateTime(value: string | null): string {
  if (!value) return '—';

  return new Date(value).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    timeZoneName: 'short',
  });
}
