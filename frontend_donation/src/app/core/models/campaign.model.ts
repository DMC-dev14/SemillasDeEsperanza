export type CampaignCategory = 'Educación' | 'Salud' | 'Vivienda' | 'Alimentación';
export type CampaignStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface CampaignResponse {
  id: number;
  title: string;
  description: string;
  history: string;
  imageUrl: string;
  category: string;
  badgeClass: string;
  location: string;
  targetAmount: number;
  amountCollected: number;
  porcentaje: number;
  donorsCount: number;
  status: CampaignStatus;
  startDate?: string;
  endDate?: string;
  createDate?: string;
  updateDate?: string;
  creatorImageUrl?: string;
  creatorName?: string;
}

export interface CampaignRequest {
  title: string;
  description: string;
  history?: string;
  imageUrl?: string;
  targetAmount: number;
  status?: CampaignStatus;
  startDate?: string;
  endDate?: string;
  category?: CampaignCategory;
}
