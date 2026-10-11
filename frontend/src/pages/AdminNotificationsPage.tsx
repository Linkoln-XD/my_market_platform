import { useEffect, useState } from 'react';
import { fetchAdminNotifications } from '../api';
import { useAuth } from '../auth';
import type { DeliveredNotification } from '../types';

export default function AdminNotificationsPage() {
  const { session } = useAuth();
  const [rows, setRows] = useState<DeliveredNotification[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) {
      return;
    }
    fetchAdminNotifications(session.accessToken)
      .then((page) => setRows(page.content))
      .catch((e: Error) => setError(e.message));
  }, [session]);

  return (
    <div className="card">
      <h2>Admin · delivered notifications</h2>
      <p className="muted">Данные из notification-service (Kafka consumer).</p>
      {error && <p className="error">{error}</p>}
      <table>
        <thead>
          <tr>
            <th>Event</th>
            <th>Order</th>
            <th>Channel</th>
            <th>At</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              <td>{row.eventType}</td>
              <td className="muted">{row.orderId.slice(0, 8)}…</td>
              <td>{row.channel}</td>
              <td>{new Date(row.deliveredAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
