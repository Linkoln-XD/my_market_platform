import type {
  AuthSession,
  Cart,
  NotificationPage,
  Order,
  OrderPage,
  Product,
  ProductPage,
} from './types';

const API = '/api/v1';
const NOTIFICATION_API = '/notification-api/v1';

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
  base: string = API,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(`${base}${path}`, { ...options, headers });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${text}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export async function fetchDemoToken(subject: string, roles: string[]): Promise<AuthSession> {
  const body = await request<{ accessToken: string }>(`/auth/demo-token`, {
    method: 'POST',
    body: JSON.stringify({ subject, roles }),
  });
  return { accessToken: body.accessToken, subject, roles };
}

export function fetchProducts(): Promise<ProductPage> {
  return request<ProductPage>(`/products?size=50`);
}

export function fetchCart(ownerId: string): Promise<Cart> {
  return request<Cart>(`/carts/${encodeURIComponent(ownerId)}`);
}

export function addToCart(ownerId: string, productId: string, quantity: number): Promise<Cart> {
  return request<Cart>(`/carts/${encodeURIComponent(ownerId)}/items`, {
    method: 'POST',
    body: JSON.stringify({ productId, quantity }),
  });
}

export function createOrder(ownerId: string, token: string, idempotencyKey: string): Promise<Order> {
  return request<Order>(
    `/orders`,
    {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ ownerId }),
    },
    token,
  );
}

export function fetchOrder(orderId: string, token: string): Promise<Order> {
  return request<Order>(`/orders/${orderId}`, {}, token);
}

export function cancelOrder(orderId: string, token: string): Promise<Order> {
  return request<Order>(
    `/orders/${orderId}/cancel`,
    { method: 'POST', body: JSON.stringify({ reason: 'Cancelled from demo UI' }) },
    token,
  );
}

export function fetchMyOrders(token: string): Promise<OrderPage> {
  return request<OrderPage>(`/orders?size=20`, {}, token);
}

export function fetchAdminOrders(token: string): Promise<OrderPage> {
  return request<OrderPage>(`/admin/orders?size=50`, {}, token);
}

export function updateStock(productId: string, stockQuantity: number, token: string): Promise<Product> {
  return request<Product>(`/products/${productId}/stock`, {
    method: 'PUT',
    body: JSON.stringify({ stockQuantity }),
  }, token);
}

export function fetchAdminNotifications(token: string): Promise<NotificationPage> {
  return request<NotificationPage>(`/admin/notifications?size=50`, {}, token, NOTIFICATION_API);
}

export function formatMoney(cents: number, currency: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(cents / 100);
}
