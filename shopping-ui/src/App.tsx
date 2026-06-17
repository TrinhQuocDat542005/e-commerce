import React, { useState, useEffect } from 'react'
import { 
  ShoppingBag, 
  RefreshCw, 
  AlertCircle, 
  LogOut, 
  Layers, 
  Activity, 
  CheckCircle2, 
  XCircle, 
  ArrowRight,
  Database
} from 'lucide-react'
import './App.css'

interface Product {
  id?: number
  name: string
  description: string
  price: number
  skuCode: string
}

interface InventoryItem {
  id?: number
  skuCode: string
  quantity: number
}

interface OrderLineItem {
  id?: number
  skuCode: string
  price: number
  quantity: number
}

interface Order {
  id?: number
  orderNumber: string
  status: string
  orderLineItemsList: OrderLineItem[]
}

export default function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('jwt_token'))

  const handleLogout = () => {
    localStorage.removeItem('jwt_token')
    setToken(null)
  }

  if (!token) {
    return <Login setToken={(t) => {
      localStorage.setItem('jwt_token', t)
      setToken(t)
    }} />
  }

  return <Dashboard token={token} onLogout={handleLogout} />
}

/* ==========================================
   LOGIN COMPONENT
   ========================================== */
interface LoginProps {
  setToken: (token: string) => void
}

function Login({ setToken }: LoginProps) {
  const [username, setUsername] = useState('testuser')
  const [password, setPassword] = useState('password')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })

      if (!res.ok) {
        throw new Error('Authentication failed. Check credentials.')
      }

      const data = await res.json()
      if (data.token) {
        setToken(data.token)
      } else {
        throw new Error('No token returned from server.')
      }
    } catch (err: any) {
      setError(err.message || 'Server connection error.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-title-section">
          <div className="login-logo">Antigravity E-Shop</div>
          <div className="login-subtitle">Transactional Outbox Pattern Visualizer</div>
        </div>

        {error && (
          <div className="error-message">
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input 
              id="username"
              type="text" 
              className="form-control"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input 
              id="password"
              type="password" 
              className="form-control"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" className="btn-login" disabled={loading}>
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  )
}

/* ==========================================
   DASHBOARD COMPONENT
   ========================================== */
interface DashboardProps {
  token: string
  onLogout: () => void
}

function Dashboard({ token, onLogout }: DashboardProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [inventory, setInventory] = useState<Record<string, number>>({})
  const [orders, setOrders] = useState<Order[]>([])
  
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({})
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [countdown, setCountdown] = useState(3)

  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }

  const fetchData = async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      // 1. Fetch Products
      const pRes = await fetch('/api/products', { headers })
      if (!pRes.ok) throw new Error('Failed to load products')
      const pData: Product[] = await pRes.json()
      setProducts(pData)

      // 2. Fetch Inventory
      const iRes = await fetch('/api/inventory/all', { headers })
      if (!iRes.ok) throw new Error('Failed to load stock levels')
      const iData: InventoryItem[] = await iRes.json()
      const invMap: Record<string, number> = {}
      iData.forEach(item => {
        invMap[item.skuCode] = item.quantity
      })
      setInventory(invMap)

      // 3. Fetch Orders
      const oRes = await fetch('/api/order', { headers })
      if (!oRes.ok) throw new Error('Failed to load orders')
      const oData: Order[] = await oRes.json()
      setOrders(oData)
      
      setErrorMessage(null)
    } catch (err: any) {
      setErrorMessage(err.message || 'Connection error with gateway.')
    } finally {
      if (!silent) setLoading(false)
    }
  }

  // Initial Fetch & Auto Refresh
  useEffect(() => {
    fetchData()
    
    // Auto Poll every 3 seconds to show outbox processing
    const pollInterval = setInterval(() => {
      fetchData(true)
      setCountdown(3)
    }, 3000)

    const timerInterval = setInterval(() => {
      setCountdown(prev => (prev > 1 ? prev - 1 : 3))
    }, 1000)

    return () => {
      clearInterval(pollInterval)
      clearInterval(timerInterval)
    }
  }, [])

  // Seeding Helper
  const handleSeedProducts = async () => {
    setLoading(true)
    try {
      const items = [
        { name: 'iPhone 15', description: 'Regular Edition. Restocked instantly on cancellation.', price: 1000.0, skuCode: 'iphone_15' },
        { name: 'iPhone 15 Pro', description: 'Premium Edition. Starts with 0 items to trigger Out-of-Stock flow.', price: 1200.0, skuCode: 'iphone_15_pro' }
      ]
      for (const item of items) {
        await fetch('/api/products', {
          method: 'POST',
          headers,
          body: JSON.stringify(item)
        })
      }
      await fetchData()
    } catch (err: any) {
      setErrorMessage('Failed to seed catalog.')
    } finally {
      setLoading(false)
    }
  }

  // Buy Event
  const handleBuy = async (skuCode: string) => {
    setActionLoading(prev => ({ ...prev, [skuCode]: true }))
    try {
      const body = {
        orderLineItemsDtoList: [{
          skuCode,
          price: skuCode === 'iphone_15' ? 1000.0 : 1200.0,
          quantity: 1
        }]
      }
      const res = await fetch('/api/order', {
        method: 'POST',
        headers,
        body: JSON.stringify(body)
      })

      if (!res.ok) {
        throw new Error('Order placement request failed.')
      }
      
      // Instantly trigger check
      await fetchData(true)
    } catch (err: any) {
      alert(err.message || 'Error occurred.')
    } finally {
      setActionLoading(prev => ({ ...prev, [skuCode]: false }))
    }
  }

  // Cancellation Event (Compensating Transaction)
  const handleCancelOrder = async (orderNumber: string) => {
    setActionLoading(prev => ({ ...prev, [orderNumber]: true }))
    try {
      const res = await fetch(`/api/order/cancel/${orderNumber}`, {
        method: 'PUT',
        headers
      })

      if (!res.ok) {
        const txt = await res.text()
        throw new Error(txt || 'Cancellation failed.')
      }

      await fetchData(true)
    } catch (err: any) {
      alert(err.message || 'Error occurred.')
    } finally {
      setActionLoading(prev => ({ ...prev, [orderNumber]: false }))
    }
  }

  return (
    <div className="app-container">
      {/* HEADER */}
      <header className="dashboard-header">
        <div className="brand-section">
          <span className="brand-logo">Antigravity E-Shop</span>
          <span className="brand-badge">Double Outbox Visualizer</span>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div className="sync-status">
            <span className="pulse-dot"></span>
            <span>Syncing live (Refresh in {countdown}s)</span>
          </div>

          <button onClick={() => fetchData()} className="sync-status" style={{ cursor: 'pointer' }} disabled={loading}>
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          </button>

          <button onClick={onLogout} className="btn-logout">
            <LogOut size={14} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {errorMessage && (
        <div className="error-message" style={{ marginBottom: '2rem' }}>
          <AlertCircle size={18} />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* DASHBOARD GRID */}
      <div className="dashboard-grid">
        {/* LEFT COLUMN */}
        <div className="main-column">
          {/* CATALOGUE */}
          <section className="glass-card">
            <h2 className="card-title">
              <ShoppingBag size={20} style={{ color: 'var(--primary)' }} />
              <span>Product Catalogue</span>
            </h2>

            {products.length === 0 ? (
              <div className="system-empty-state">
                <Database size={32} style={{ marginBottom: '10px', opacity: 0.3 }} />
                <p>Catalog is empty. Seed local databases to start.</p>
                <button onClick={handleSeedProducts} className="btn-seed">
                  Seed Products Database
                </button>
              </div>
            ) : (
              <div className="products-grid">
                {products.map(p => {
                  const qty = inventory[p.skuCode] ?? 0
                  const isOutOfStock = qty <= 0
                  return (
                    <div className="product-card" key={p.skuCode}>
                      <div className="product-header">
                        <h3 className="product-name">{p.name}</h3>
                        <span className="product-sku">{p.skuCode}</span>
                      </div>
                      <p className="product-desc">{p.description}</p>
                      <div className="product-footer">
                        <span className="product-price">${p.price}</span>
                        <button 
                          className="btn-buy" 
                          onClick={() => handleBuy(p.skuCode)}
                          disabled={actionLoading[p.skuCode] || isOutOfStock}
                        >
                          {actionLoading[p.skuCode] ? 'Ordering...' : isOutOfStock ? 'Out of Stock' : 'Order Now'}
                          <ArrowRight size={14} />
                        </button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </section>

          {/* ACTIVE ORDERS */}
          <section className="glass-card">
            <h2 className="card-title">
              <Activity size={20} style={{ color: 'var(--secondary)' }} />
              <span>Live Outbox Transaction Logger</span>
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.4)', marginTop: '-10px', marginBottom: '1.5rem' }}>
              Observe order state changes. Saga deduces stock (<strong>PENDING ➜ CONFIRMED</strong>) via Kafka. Cancellation performs restocking (<strong>CONFIRMED ➜ CANCELLED</strong>).
            </p>

            <div className="orders-table-wrapper">
              {orders.length === 0 ? (
                <div className="system-empty-state">No transactions recorded. Buy a product to initiate.</div>
              ) : (
                <table className="orders-table">
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>Product / SKU</th>
                      <th>Qty</th>
                      <th>Price</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map(o => {
                      const item = o.orderLineItemsList[0]
                      const orderStatus = o.status || 'PENDING'
                      const isPending = orderStatus === 'PENDING'
                      const isConfirmed = orderStatus === 'CONFIRMED'
                      const isCancelled = orderStatus === 'CANCELLED'

                      return (
                        <tr className="order-row" key={o.orderNumber}>
                          <td>
                            <span className="order-number-val" title={o.orderNumber}>
                              {o.orderNumber.substring(0, 8)}...
                            </span>
                          </td>
                          <td style={{ fontWeight: 600 }}>{item?.skuCode || 'UNKNOWN'}</td>
                          <td style={{ fontFamily: 'monospace' }}>{item?.quantity || 0}</td>
                          <td style={{ color: 'var(--secondary)', fontWeight: 600 }}>
                            ${item ? item.price * item.quantity : 0}
                          </td>
                          <td>
                            <span className={`status-pill ${orderStatus.toLowerCase()}`}>
                              {isConfirmed && <CheckCircle2 size={12} style={{ marginRight: 4 }} />}
                              {isCancelled && <XCircle size={12} style={{ marginRight: 4 }} />}
                              {isPending && <RefreshCw size={12} className="animate-spin" style={{ marginRight: 4 }} />}
                              {orderStatus}
                            </span>
                          </td>
                          <td>
                            <button
                              className="btn-cancel"
                              onClick={() => handleCancelOrder(o.orderNumber)}
                              disabled={!isConfirmed || actionLoading[o.orderNumber]}
                            >
                              {actionLoading[o.orderNumber] ? 'Cancelling...' : 'Cancel Order'}
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </section>
        </div>

        {/* RIGHT COLUMN */}
        <div className="sidebar-column">
          {/* INVENTORY MONITOR */}
          <section className="glass-card">
            <h2 className="card-title">
              <Layers size={20} style={{ color: 'var(--accent-blue)' }} />
              <span>Real-Time Warehouse Stock</span>
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.4)', marginTop: '-10px', marginBottom: '1.5rem' }}>
              Database levels inside <code>inventory_db</code>.
            </p>

            <div className="inventory-list">
              {Object.keys(inventory).length === 0 ? (
                <div className="system-empty-state">No inventory metadata found.</div>
              ) : (
                Object.entries(inventory).map(([sku, qty]) => (
                  <div className="inventory-item" key={sku}>
                    <div className="inventory-info">
                      <span className="inventory-sku">{sku}</span>
                      <span className="inventory-stock-lbl">sku code identifier</span>
                    </div>
                    <div className="inventory-quantity-badge">
                      <span className={`status-indicator ${qty > 0 ? 'in-stock' : 'out-of-stock'}`}></span>
                      <span className="quantity-value">{qty}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>

          {/* EXPLANATION */}
          <section className="glass-card" style={{ background: 'rgba(139, 92, 246, 0.03)', borderColor: 'rgba(139, 92, 246, 0.15)' }}>
            <h3 style={{ margin: '0 0 10px 0', fontSize: '1rem', color: '#fff' }}>How it works under the hood</h3>
            <ul style={{ paddingLeft: '1.2rem', margin: 0, fontSize: '0.82rem', color: 'rgba(255,255,255,0.6)', lineHeight: '1.5' }}>
              <li style={{ marginBottom: 8 }}>
                <strong>Transactional Outbox:</strong> Placing an order updates the Order database and inserts an event in the Outbox within <em>one atomic transaction</em>.
              </li>
              <li style={{ marginBottom: 8 }}>
                <strong>Guaranteed Delivery:</strong> The Outbox Scheduler polls pending events, publishes to Kafka, and updates status to <code>PROCESSED</code>.
              </li>
              <li>
                <strong>Compensating Transaction:</strong> Order cancellation immediately changes status to <code>CANCELLED</code>, writing an Outbox record. Kafka routes this to Inventory to increment back the stock level.
              </li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  )
}
