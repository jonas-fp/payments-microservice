export type PaymentStatus =
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'PARTIALLY_REFUNDED'
  | 'FULLY_REFUNDED'
  | 'VOIDED';

export type PaymentEventType =
  | 'AUTHORIZE_REQUESTED'
  | 'AUTHORIZE_SUCCESS'
  | 'AUTHORIZE_FAILED'
  | 'AUTHORIZE_VOIDED'
  | 'CAPTURE_REQUESTED'
  | 'CAPTURE_SUCCESS'
  | 'CAPTURE_FAILED'
  | 'CAPTURE_VOIDED'
  | 'REFUND_REQUESTED'
  | 'REFUND_SUCCESS'
  | 'REFUND_FAILED'
  | 'REFUND_VOIDED';

export interface PaymentEvent {
  id: string;
  eventType: PaymentEventType;
  processorEventReference: string;
  createdAt: string;
}

export interface PaymentDetailsResponse {
  id: string;
  customerId: string;
  invoiceId: string;
  authorizedAmount: number;
  capturedAmount: number;
  refundedAmount: number;
  currency: string;
  status: PaymentStatus;
  processorReference: string;
  createdAt: string;
  history: PaymentEvent[];
}
