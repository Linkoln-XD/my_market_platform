export type Product = {
  id: string;
  name: string;
  description: string;
  priceCents: number;
  currency: string;
  stockQuantity: number;
  categoryId: string;
  active: boolean;
};

export type ProductPage = {
  content: Product[];
  totalElements: number;
};

export type CartItem = {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
};

export type Cart = {
  ownerId: string;
  items: CartItem[];
  subtotalCents: number;
  currency: string;
};

export type OrderItem = {
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  unitPriceCents: number;
  lineTotalCents: number;
};

export type Order = {
  id: string;
  ownerId: string;
  status: string;
  currency: string;
  subtotalCents: number;
  items: OrderItem[];
  createdAt: string;
};

export type OrderPage = {
  content: Order[];
  totalElements: number;
};

export type DeliveredNotification = {
  id: string;
  eventId: string;
  orderId: string;
  eventType: string;
  channel: string;
  message: string;
  deliveredAt: string;
};

export type NotificationPage = {
  content: DeliveredNotification[];
  totalElements: number;
};

export type AuthSession = {
  accessToken: string;
  subject: string;
  roles: string[];
};
