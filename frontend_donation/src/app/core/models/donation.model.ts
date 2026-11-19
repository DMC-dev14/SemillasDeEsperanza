export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REFUNDED';

export type PaymentMethod =
  | 'CREDIT_CARD'
  | 'DEBIT_CARD'
  | 'BANK_TRANSFER'
  | 'PAYPAL'
  | 'YAPE'
  | 'PLIN'
  | 'CASH';

export interface DonationRequest {
  campaignId: number;
  amount: number;
  donorName: string;
  donorEmail: string;
  donorPhone?: string;
  paymentMethod: string;
  donationType?: string;
  message?: string;
}

export interface DonationResponse {
  id: number;
  amount: number;
  currency: string;
  message: string;
  status: PaymentStatus;
  donationDate: string;
  donorId: number | null;
  donorName: string;
  donorEmail: string;
  donorImageUrl?: string;
  campaignId: number;
  campaignTitle: string;
  methodPay: PaymentMethod | string;
  codeReferential: string;
  voucherNumber: string | null;
}

export interface DonationCreatedResponse {
  donation: DonationResponse;
  checkoutToken: string;
}

export interface ClientDashboardSummary {
  totalDonado: number;
  cantidadDonaciones: number;
  proyectosApoyados: number;
}

export interface TopDonor {
  rank: number;
  badge: string;
  name: string;
  donations: number;
  total: number;
  donorImageUrl?: string;
}

export interface MyDonationsResponse {
  summary: ClientDashboardSummary;
  donations: DonationResponse[];
  topDonors: TopDonor[];
  recentFeed: DonationResponse[];
}

export interface PublicFeedResponse {
  topDonors: TopDonor[];
  recentFeed: DonationResponse[];
}

export interface YapeTokenRequestDTO {
  donationId: number;
  checkoutToken: string;
  phoneNumber: string;
  otp: string;
}

export interface YapeTokenResponse {
  token: string;
}

export interface ChargePaymentRequestDTO {
  donationId: number;
  token: string;
  checkoutToken: string;
  phoneNumber?: string;
  deviceSessionId?: string;
}

export interface ChargePaymentResponse {
  id: string;
  status: string;
  status_detail: string;
  transaction_amount: string;
}

export interface CreateOrderPayPalRequest {
  donationId: number;
  checkoutToken: string;
}

export interface CreateOrderPayPalResponse {
  orderId: string;
  status: string;
  approvalUrl: string;
  amountUsd: number;
}

export interface CaptureOrderPayPalRequest {
  donationId: number;
  checkoutToken: string;
}
