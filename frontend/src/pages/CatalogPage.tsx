import { useEffect, useState } from 'react';
import { addToCart, fetchProducts, formatMoney } from '../api';
import { useAuth } from '../auth';
import type { Product } from '../types';

export default function CatalogPage() {
  const { session, loginAsUser, loginAsAdmin } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  useEffect(() => {
    fetchProducts()
      .then((page) => setProducts(page.content))
      .catch((e: Error) => setError(e.message));
  }, []);

  async function ensureUser() {
    if (session) {
      return session;
    }
    await loginAsUser('demo-shopper');
    return loadStoredSession();
  }

  function loadStoredSession() {
    const raw = sessionStorage.getItem('market.demo.auth');
    if (!raw) {
      throw new Error('No session');
    }
    return JSON.parse(raw) as { subject: string };
  }

  async function handleAdd(product: Product) {
    setError(null);
    setBusy(product.id);
    try {
      const auth = await ensureUser();
      await addToCart(auth.subject, product.id, 1);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  }

  return (
    <div>
      {!session && (
        <div className="card">
          <p>Для корзины и заказа нужен JWT (demo).</p>
          <button type="button" onClick={() => loginAsUser('demo-shopper')}>
            Войти как покупатель
          </button>{' '}
          <button type="button" className="secondary" onClick={() => loginAsAdmin('demo-admin')}>
            Войти как admin
          </button>
        </div>
      )}
      {error && <p className="error">{error}</p>}
      <div className="grid">
        {products.map((p) => (
          <div className="card" key={p.id}>
            <h3>{p.name}</h3>
            <p className="muted">{p.description}</p>
            <p>
              <strong>{formatMoney(p.priceCents, p.currency)}</strong>
              <span className="muted"> · stock {p.stockQuantity}</span>
            </p>
            <button type="button" disabled={busy === p.id} onClick={() => handleAdd(p)}>
              В корзину
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
