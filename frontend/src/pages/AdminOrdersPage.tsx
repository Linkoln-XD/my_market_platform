import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchAdminOrders, formatMoney } from '../api';
import { useAuth } from '../auth';
import type { Order } from '../types';

export default function AdminOrdersPage() {
  const { session } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) {
      return;
    }
    fetchAdminOrders(session.accessToken)
      .then((page) => setOrders(page.content))
      .catch((e: Error) => setError(e.message));
  }, [session]);

  return (
    <div className="card">
      <h2>Admin · заказы</h2>
      {error && <p className="error">{error}</p>}
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Owner</th>
            <th>Status</th>
            <th>Sum</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td className="muted">{o.id.slice(0, 8)}…</td>
              <td>{o.ownerId}</td>
              <td>
                <span className="badge">{o.status}</span>
              </td>
              <td>{formatMoney(o.subtotalCents, o.currency)}</td>
              <td>
                <Link to={`/orders/${o.id}`}>open</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
