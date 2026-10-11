import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { cancelOrder, fetchOrder, formatMoney } from '../api';
import { useAuth } from '../auth';
import type { Order } from '../types';

export default function OrderPage() {
  const { orderId } = useParams();
  const { session } = useAuth();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session || !orderId) {
      return;
    }
    let cancelled = false;
    const tick = async () => {
      try {
        const data = await fetchOrder(orderId, session.accessToken);
        if (!cancelled) {
          setOrder(data);
          setError(null);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : String(e));
        }
      }
    };
    tick();
    const id = window.setInterval(tick, 2000);
    return () => {
      cancelled = true;
      window.clearInterval(id);
    };
  }, [session, orderId]);

  async function handleCancel() {
    if (!session || !orderId) {
      return;
    }
    try {
      const updated = await cancelOrder(orderId, session.accessToken);
      setOrder(updated);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  if (!session) {
    return (
      <div className="card">
        <p>Нужна авторизация.</p>
        <Link to="/">На главную</Link>
      </div>
    );
  }

  if (!order) {
    return <div className="card">{error ? <p className="error">{error}</p> : 'Загрузка…'}</div>;
  }

  const canCancel = order.status === 'PENDING_PAYMENT' || order.status === 'PAID';

  return (
    <div className="card">
      <h2>
        Заказ <span className="badge">{order.status}</span>
      </h2>
      <p className="muted">id: {order.id}</p>
      <p>Owner: {order.ownerId}</p>
      <ul>
        {order.items.map((item) => (
          <li key={item.productId}>
            {item.productName} × {item.quantity} — {formatMoney(item.lineTotalCents, order.currency)}
          </li>
        ))}
      </ul>
      <p>
        <strong>{formatMoney(order.subtotalCents, order.currency)}</strong>
      </p>
      {order.status === 'PENDING_PAYMENT' && (
        <p className="muted">Mock payment через Kafka… статус обновится автоматически.</p>
      )}
      {canCancel && (
        <button type="button" className="secondary" onClick={handleCancel}>
          Отменить заказ
        </button>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
