import type { ReactNode } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import AdminNotificationsPage from './pages/AdminNotificationsPage';
import AdminOrdersPage from './pages/AdminOrdersPage';
import AdminProductsPage from './pages/AdminProductsPage';
import CartPage from './pages/CartPage';
import CatalogPage from './pages/CatalogPage';
import OrderPage from './pages/OrderPage';

function Shell({ children }: { children: ReactNode }) {
  const { session, logout, isAdmin } = useAuth();
  return (
    <div className="layout">
      <header>
        <h1>Market Platform Demo</h1>
        <nav>
          <Link to="/">Каталог</Link>
          <Link to="/cart">Корзина</Link>
          {isAdmin && (
            <>
              <Link to="/admin/products">Admin · товары</Link>
              <Link to="/admin/orders">Admin · заказы</Link>
              <Link to="/admin/notifications">Admin · уведомления</Link>
            </>
          )}
        </nav>
        <div className="muted">
          {session ? (
            <>
              {session.subject} ({session.roles.join(', ')}){' '}
              <button type="button" className="secondary" onClick={logout}>
                Выйти
              </button>
            </>
          ) : (
            'Не авторизован'
          )}
        </div>
      </header>
      {children}
    </div>
  );
}

function RequireAdmin({ children }: { children: ReactNode }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <Shell>
      <Routes>
        <Route path="/" element={<CatalogPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/orders/:orderId" element={<OrderPage />} />
        <Route
          path="/admin/products"
          element={
            <RequireAdmin>
              <AdminProductsPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/orders"
          element={
            <RequireAdmin>
              <AdminOrdersPage />
            </RequireAdmin>
          }
        />
        <Route
          path="/admin/notifications"
          element={
            <RequireAdmin>
              <AdminNotificationsPage />
            </RequireAdmin>
          }
        />
      </Routes>
    </Shell>
  );
}
