import { useEffect, useState } from 'react';
import { fetchProducts, formatMoney, updateStock } from '../api';
import { useAuth } from '../auth';
import type { Product } from '../types';

export default function AdminProductsPage() {
  const { session } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProducts()
      .then((p) => setProducts(p.content))
      .catch((e: Error) => setError(e.message));
  }, []);

  async function saveStock(product: Product, value: string) {
    if (!session) {
      return;
    }
    const stockQuantity = Number.parseInt(value, 10);
    if (Number.isNaN(stockQuantity)) {
      return;
    }
    try {
      const updated = await updateStock(product.id, stockQuantity, session.accessToken);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? { ...p, ...updated } : p)));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  return (
    <div className="card">
      <h2>Admin · остатки</h2>
      {error && <p className="error">{error}</p>}
      <table>
        <thead>
          <tr>
            <th>Товар</th>
            <th>Цена</th>
            <th>Stock</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>{p.name}</td>
              <td>{formatMoney(p.priceCents, p.currency)}</td>
              <td>
                <input
                  type="number"
                  defaultValue={p.stockQuantity}
                  onBlur={(e) => saveStock(p, e.target.value)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
