import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createOrder, fetchCart, formatMoney } from '../api';
import { useAuth } from '../auth';
import type { Cart } from '../types';

export default function CartPage() {
  const { session, loginAsUser } = useAuth();
  const navigate = useNavigate();
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checkingOut, setCheckingOut] = useState(false);

  const load = useCallback(async () => {
    if (!session) {
      setCart(null);
      return;
    }
    try {
      const data = await fetchCart(session.subject);
      setCart(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [session]);

  useEffect(() => {
    load();
  }, [load]);

  async function checkout() {
    if (!session) {
      await loginAsUser('demo-shopper');
    }
    const raw = sessionStorage.getItem('market.demo.auth');
    if (!raw) {
      setError('Нет сессии');
      return;
    }
    const auth = JSON.parse(raw) as { accessToken: string; subject: string };
    setCheckingOut(true);
    setError(null);
    try {
      const idempotencyKey = `ui-${Date.now()}`;
      const order = await createOrder(auth.subject, auth.accessToken, idempotencyKey);
      navigate(`/orders/${order.id}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setCheckingOut(false);
    }
  }

  if (!session) {
    return (
      <div className="card">
        <p>Сначала войдите как покупатель на главной.</p>
        <button type="button" onClick={() => loginAsUser('demo-shopper')}>
          Войти
        </button>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Корзина · {session.subject}</h2>
      {error && <p className="error">{error}</p>}
      {!cart || cart.items.length === 0 ? (
        <p className="muted">Пусто. <Link to="/">В каталог</Link></p>
      ) : (
        <>
          <table>
            <thead>
              <tr>
                <th>Товар</th>
                <th>Qty</th>
                <th>Сумма</th>
              </tr>
            </thead>
            <tbody>
              {cart.items.map((item) => (
                <tr key={item.productId}>
                  <td>{item.productName}</td>
                  <td>{item.quantity}</td>
                  <td>{formatMoney(item.lineTotalCents, cart.currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p>
            <strong>Итого: {formatMoney(cart.subtotalCents, cart.currency)}</strong>
          </p>
          <button type="button" disabled={checkingOut} onClick={checkout}>
            Оформить заказ (Kafka payment)
          </button>
        </>
      )}
    </div>
  );
}
