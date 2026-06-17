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
  Database,
  Wallet,
  ShoppingCart,
  Trash2,
  Plus,
  Minus,
  Bell,
  Info
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

interface CartItem {
  product: Product
  quantity: number
}

function Dashboard({ token, onLogout }: DashboardProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [inventory, setInventory] = useState<Record<string, number>>({})
  const [orders, setOrders] = useState<Order[]>([])
  const [walletBalance, setWalletBalance] = useState<number | null>(null)
  
  const [cart, setCart] = useState<Record<string, CartItem>>({})
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({})
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [countdown, setCountdown] = useState(3)

  // Real-time notifications via SSE
  const [toasts, setToasts] = useState<Array<{ id: string; message: string; type: 'info' | 'success' | 'error' }>>([])
  const [notifications, setNotifications] = useState<Array<{ id: string; message: string; type: 'info' | 'success' | 'error'; timestamp: Date; read: boolean; traceId?: string }>>([])
  const [showActivityLog, setShowActivityLog] = useState(false)

  const unreadCount = notifications.filter(n => !n.read).length

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

      // 4. Fetch Wallet Balance
      try {
        const wRes = await fetch('/api/payment/balance', { headers })
        if (wRes.ok) {
          const wData = await wRes.json()
          setWalletBalance(wData.balance)
        }
      } catch (e) {
        console.error('Failed to load wallet balance', e)
      }
      
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

  // Connect to SSE notification stream
  useEffect(() => {
    console.log("🔌 Connecting to notification SSE stream...")
    const eventSource = new EventSource('/api/notifications/stream')

    eventSource.addEventListener('connect', (e: any) => {
      console.log('🔌 SSE Stream Connection established:', e.data)
    })

    eventSource.addEventListener('notification', (e: any) => {
      console.log('📢 Received SSE notification:', e.data)
      try {
        const payload = JSON.parse(e.data)
        const id = Math.random().toString(36).substring(2, 9)
        
        const newToast = { id, message: payload.message, type: payload.type as 'info' | 'success' | 'error' }
        setToasts(prev => [...prev, newToast])
        
        const newNotification = {
          id,
          message: payload.message,
          type: payload.type as 'info' | 'success' | 'error',
          timestamp: new Date(),
          read: false,
          traceId: payload.traceId
        }
        setNotifications(prev => [newNotification, ...prev])

        // Auto remove toast after 4.5 seconds
        setTimeout(() => {
          setToasts(prev => prev.filter(t => t.id !== id))
        }, 4500)
      } catch (err) {
        console.error('Failed to parse SSE notification payload', err)
      }
    })

    eventSource.onerror = (e) => {
      console.error('❌ SSE Stream error:', e)
    }

    return () => {
      console.log("🔌 Closing notification SSE stream...")
      eventSource.close()
    }
  }, [])

  // Close activity log dropdown on outside click
  useEffect(() => {
    if (!showActivityLog) return
    const handleDocumentClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement
      if (!target.closest('.activity-bell-container')) {
        setShowActivityLog(false)
      }
    }
    document.addEventListener('click', handleDocumentClick)
    return () => document.removeEventListener('click', handleDocumentClick)
  }, [showActivityLog])

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

  // Shopping Cart Handlers
  const handleAddToCart = (product: Product) => {
    setCart(prev => {
      const existing = prev[product.skuCode]
      const currentQty = existing ? existing.quantity : 0
      const availableStock = inventory[product.skuCode] ?? 0
      if (currentQty >= availableStock) {
        alert(`Không thể thêm. Chỉ còn ${availableStock} sản phẩm trong kho.`)
        return prev
      }
      return {
        ...prev,
        [product.skuCode]: {
          product,
          quantity: currentQty + 1
        }
      }
    })
  }

  const handleUpdateCartQty = (skuCode: string, delta: number) => {
    setCart(prev => {
      const existing = prev[skuCode]
      if (!existing) return prev
      const newQty = existing.quantity + delta
      const availableStock = inventory[skuCode] ?? 0
      if (newQty <= 0) {
        const { [skuCode]: _, ...rest } = prev
        return rest
      }
      if (newQty > availableStock) {
        alert(`Không thể tăng thêm. Chỉ còn ${availableStock} sản phẩm trong kho.`)
        return prev
      }
      return {
        ...prev,
        [skuCode]: {
          ...existing,
          quantity: newQty
        }
      }
    })
  }

  const handleRemoveFromCart = (skuCode: string) => {
    setCart(prev => {
      const { [skuCode]: _, ...rest } = prev
      return rest
    })
  }

  const calculateCartTotal = () => {
    return Object.values(cart).reduce((sum, item) => sum + (item.product.price * item.quantity), 0)
  }

  const handleCheckout = async () => {
    setLoading(true)
    try {
      const itemsList = Object.values(cart).map(item => ({
        skuCode: item.product.skuCode,
        price: item.product.price,
        quantity: item.quantity
      }))

      const body = {
        orderLineItemsDtoList: itemsList
      }

      const res = await fetch('/api/order', {
        method: 'POST',
        headers,
        body: JSON.stringify(body)
      })

      if (!res.ok) {
        throw new Error('Gửi đơn hàng thất bại.')
      }

      setCart({})
      await fetchData(true)
    } catch (err: any) {
      alert(err.message || 'Lỗi xảy ra trong quá trình thanh toán.')
    } finally {
      setLoading(false)
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
          {/* ACTIVITY BELL */}
          <div className="activity-bell-container" style={{ position: 'relative' }}>
            <button 
              className={`btn-bell ${unreadCount > 0 ? 'pulse-bell' : ''}`} 
              onClick={() => {
                setShowActivityLog(prev => !prev)
                if (!showActivityLog) {
                  // Mark all as read when opening dropdown
                  setNotifications(prev => prev.map(n => ({ ...n, read: true })))
                }
              }}
              style={{
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid rgba(255, 255, 255, 0.05)',
                color: 'rgba(255, 255, 255, 0.7)',
                padding: '8px',
                borderRadius: '50%',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                transition: 'all 0.2s'
              }}
            >
              <Bell size={16} />
              {unreadCount > 0 && (
                <span 
                  className="bell-badge"
                  style={{
                    position: 'absolute',
                    top: '-4px',
                    right: '-4px',
                    background: 'var(--secondary)',
                    color: '#fff',
                    borderRadius: '50%',
                    fontSize: '0.7rem',
                    minWidth: '16px',
                    height: '16px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 'bold',
                    padding: '2px'
                  }}
                >
                  {unreadCount}
                </span>
              )}
            </button>
            
            {showActivityLog && (
              <div 
                className="activity-dropdown glass-card"
                style={{
                  position: 'absolute',
                  top: '100%',
                  right: 0,
                  marginTop: '10px',
                  width: '320px',
                  zIndex: 100,
                  padding: '12px',
                  maxHeight: '400px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px'
                }}
              >
                <div 
                  className="activity-dropdown-header"
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                    paddingBottom: '8px',
                    marginBottom: '4px'
                  }}
                >
                  <h3 style={{ margin: 0, fontSize: '0.95rem', fontWeight: 600, color: '#fff' }}>Hoạt động gần đây</h3>
                  {notifications.length > 0 && (
                    <button 
                      className="btn-clear-history" 
                      onClick={() => setNotifications([])}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        color: 'rgba(255, 255, 255, 0.4)',
                        fontSize: '0.75rem',
                        cursor: 'pointer',
                        padding: 0
                      }}
                    >
                      Xóa tất cả
                    </button>
                  )}
                </div>
                <div 
                  className="activity-dropdown-body"
                  style={{
                    overflowY: 'auto',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                    paddingRight: '4px'
                  }}
                >
                  {notifications.length === 0 ? (
                    <div className="activity-empty" style={{ textAlign: 'center', padding: '20px 0', fontSize: '0.8rem', color: 'rgba(255, 255, 255, 0.4)' }}>
                      Không có hoạt động nào.
                    </div>
                  ) : (
                    notifications.map(n => (
                      <div 
                        key={n.id} 
                        className={`activity-item ${n.type}`}
                        style={{
                          display: 'flex',
                          gap: '10px',
                          padding: '8px',
                          borderRadius: '6px',
                          background: n.type === 'success' ? 'rgba(16, 185, 129, 0.03)' : n.type === 'error' ? 'rgba(239, 68, 68, 0.03)' : 'rgba(255, 255, 255, 0.01)',
                          borderLeft: `3px solid ${n.type === 'success' ? 'var(--status-confirmed)' : n.type === 'error' ? 'var(--status-cancelled)' : 'var(--primary)'}`
                        }}
                      >
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1 }}>
                          <p style={{ margin: 0, fontSize: '0.8rem', lineHeight: '1.4', color: 'rgba(255, 255, 255, 0.8)' }}>{n.message}</p>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '2px' }}>
                            <span style={{ fontSize: '0.7rem', color: 'rgba(255, 255, 255, 0.35)', fontFamily: 'monospace' }}>
                              {n.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                            </span>
                            {n.traceId && (
                              <span 
                                title="Click to copy Trace ID"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  navigator.clipboard.writeText(n.traceId || "")
                                  alert(`Copied Trace ID: ${n.traceId}`)
                                }}
                                style={{ 
                                  fontSize: '0.65rem', 
                                  color: 'var(--primary)', 
                                  fontFamily: 'monospace', 
                                  cursor: 'pointer',
                                  background: 'rgba(139, 92, 246, 0.15)',
                                  padding: '1px 5px',
                                  borderRadius: '4px'
                                }}
                              >
                                trace:{n.traceId.substring(0, 8)}...
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

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
                    <div className="product-card" key={p.id ?? p.skuCode}>
                      <div className="product-header">
                        <h3 className="product-name">{p.name}</h3>
                        <span className="product-sku">{p.skuCode}</span>
                      </div>
                      <p className="product-desc">{p.description}</p>
                      <div className="product-footer">
                        <span className="product-price">${p.price.toFixed(2)}</span>
                        <button 
                          className="btn-buy" 
                          onClick={() => handleAddToCart(p)}
                          disabled={isOutOfStock}
                        >
                          {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
                          <ShoppingCart size={14} style={{ marginLeft: 4 }} />
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
                      <th>Items (SKU x Qty)</th>
                      <th>Total Price</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map(o => {
                      const orderStatus = o.status || 'PENDING'
                      const isPending = orderStatus === 'PENDING' || orderStatus === 'PENDING_PAYMENT'
                      const isConfirmed = orderStatus === 'CONFIRMED'
                      const isCancelled = orderStatus === 'CANCELLED'
                      const totalPrice = o.orderLineItemsList.reduce((sum, item) => sum + (item.price * item.quantity), 0)

                      return (
                        <tr className="order-row" key={o.orderNumber}>
                          <td>
                            <span className="order-number-val" title={o.orderNumber}>
                              {o.orderNumber.substring(0, 8)}...
                            </span>
                          </td>
                          <td>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                              {o.orderLineItemsList.map((item, idx) => (
                                <div key={idx} style={{ fontSize: '0.85rem' }}>
                                  <span style={{ fontWeight: 600 }}>{item.skuCode}</span>
                                  <span style={{ opacity: 0.4, margin: '0 4px' }}>x</span>
                                  <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{item.quantity}</span>
                                </div>
                              ))}
                            </div>
                          </td>
                          <td style={{ color: 'var(--secondary)', fontWeight: 600 }}>
                            ${totalPrice.toFixed(2)}
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
          {/* SHOPPING CART */}
          <section className="glass-card">
            <h2 className="card-title">
              <ShoppingCart size={20} style={{ color: 'var(--primary)' }} />
              <span>Shopping Cart</span>
              {Object.keys(cart).length > 0 && (
                <span style={{
                  background: 'var(--primary)',
                  color: '#fff',
                  borderRadius: '50%',
                  padding: '2px 8px',
                  fontSize: '0.75rem',
                  fontWeight: 'bold',
                  marginLeft: 'auto'
                }}>
                  {Object.values(cart).reduce((sum, item) => sum + item.quantity, 0)}
                </span>
              )}
            </h2>
            
            {Object.keys(cart).length === 0 ? (
              <div className="system-empty-state" style={{ padding: '20px 0' }}>
                Giỏ hàng trống. Hãy chọn sản phẩm.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '250px', overflowY: 'auto', paddingRight: '5px' }}>
                  {Object.values(cart).map(item => {
                    const sku = item.product.skuCode
                    const qty = item.quantity
                    const availableStock = inventory[sku] ?? 0
                    return (
                      <div key={sku} style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px',
                        background: 'rgba(255, 255, 255, 0.03)',
                        borderRadius: '8px',
                        border: '1px solid rgba(255, 255, 255, 0.05)'
                      }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                          <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{item.product.name}</span>
                          <span style={{ fontSize: '0.8rem', color: 'var(--secondary)' }}>${item.product.price.toFixed(2)}</span>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <button 
                            onClick={() => handleUpdateCartQty(sku, -1)}
                            style={{
                              width: '24px',
                              height: '24px',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              background: 'rgba(255,255,255,0.05)',
                              border: '1px solid rgba(255,255,255,0.1)',
                              borderRadius: '4px',
                              color: '#fff',
                              cursor: 'pointer'
                            }}
                          >
                            <Minus size={10} />
                          </button>
                          <span style={{ fontFamily: 'monospace', minWidth: '15px', textAlign: 'center', fontWeight: 'bold' }}>{qty}</span>
                          <button 
                            onClick={() => handleUpdateCartQty(sku, 1)}
                            disabled={qty >= availableStock}
                            style={{
                              width: '24px',
                              height: '24px',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              background: 'rgba(255,255,255,0.05)',
                              border: '1px solid rgba(255,255,255,0.1)',
                              borderRadius: '4px',
                              color: '#fff',
                              cursor: qty >= availableStock ? 'not-allowed' : 'pointer',
                              opacity: qty >= availableStock ? 0.3 : 1
                            }}
                          >
                            <Plus size={10} />
                          </button>
                          <button 
                            onClick={() => handleRemoveFromCart(sku)}
                            style={{
                              width: '24px',
                              height: '24px',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              background: 'rgba(239, 68, 68, 0.1)',
                              border: '1px solid rgba(239, 68, 68, 0.2)',
                              borderRadius: '4px',
                              color: '#ef4444',
                              cursor: 'pointer',
                              marginLeft: '4px'
                            }}
                          >
                            <Trash2 size={10} />
                          </button>
                        </div>
                      </div>
                    )
                  })}
                </div>
                
                <div style={{
                  borderTop: '1px dashed rgba(255,255,255,0.1)',
                  paddingTop: '15px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 'bold' }}>
                    <span>Tổng tiền:</span>
                    <span style={{ color: 'var(--secondary)', fontSize: '1.1rem' }}>${calculateCartTotal().toFixed(2)}</span>
                  </div>
                  <button 
                    onClick={handleCheckout} 
                    disabled={loading || Object.keys(cart).length === 0}
                    style={{
                      background: 'linear-gradient(135deg, var(--primary), #7c3aed)',
                      border: 'none',
                      borderRadius: '8px',
                      color: '#fff',
                      padding: '12px',
                      fontWeight: 'bold',
                      cursor: 'pointer',
                      boxShadow: '0 4px 12px rgba(139, 92, 246, 0.25)',
                      transition: 'all 0.2s',
                      opacity: loading || Object.keys(cart).length === 0 ? 0.5 : 1
                    }}
                  >
                    {loading ? 'Đang xử lý...' : 'Đặt hàng ngay'}
                  </button>
                </div>
              </div>
            )}
          </section>

          {/* USER WALLET BALANCE */}
          <section className="glass-card">
            <h2 className="card-title">
              <Wallet size={20} style={{ color: 'var(--status-pending)' }} />
              <span>User Wallet Balance</span>
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'rgba(255,255,255,0.4)', marginTop: '-10px', marginBottom: '1.5rem' }}>
              Current balance for user <code>testuser</code> inside <code>payment_db</code>.
            </p>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', padding: '10px 0' }}>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--status-pending)' }}>
                {walletBalance !== null ? `$${walletBalance.toFixed(2)}` : 'Loading...'}
              </span>
              {walletBalance !== null && <span style={{ fontSize: '0.9rem', color: 'rgba(255, 255, 255, 0.4)' }}>USD</span>}
            </div>
          </section>

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

      {/* FLOATING TOASTS */}
      <div 
        className="toast-container"
        style={{
          position: 'fixed',
          bottom: '24px',
          right: '24px',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          maxWidth: '380px',
          width: '100%',
          pointerEvents: 'none'
        }}
      >
        {toasts.map(toast => (
          <div 
            key={toast.id} 
            className={`toast-alert ${toast.type}`}
            style={{
              pointerEvents: 'auto',
              background: 'rgba(15, 17, 26, 0.85)',
              backdropFilter: 'blur(12px)',
              border: `1px solid ${toast.type === 'success' ? 'rgba(16, 185, 129, 0.3)' : toast.type === 'error' ? 'rgba(239, 68, 68, 0.3)' : 'rgba(139, 92, 246, 0.3)'}`,
              borderRadius: '10px',
              padding: '12px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              boxShadow: '0 8px 30px rgba(0, 0, 0, 0.3)',
              animation: 'slideInRight 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards',
              position: 'relative',
              overflow: 'hidden'
            }}
          >
            {/* Color accent bar on the left */}
            <div 
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                bottom: 0,
                width: '4px',
                background: toast.type === 'success' ? 'var(--status-confirmed)' : toast.type === 'error' ? 'var(--status-cancelled)' : 'var(--primary)'
              }}
            />
            
            <div 
              style={{
                color: toast.type === 'success' ? 'var(--status-confirmed)' : toast.type === 'error' ? 'var(--status-cancelled)' : 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              {toast.type === 'success' && <CheckCircle2 size={18} />}
              {toast.type === 'error' && <XCircle size={18} />}
              {toast.type === 'info' && <Info size={18} />}
            </div>
            
            <div style={{ flex: 1, fontSize: '0.85rem', color: '#fff', fontWeight: 500, lineHeight: 1.4 }}>
              {toast.message}
            </div>
            
            <button 
              onClick={() => setToasts(prev => prev.filter(t => t.id !== toast.id))}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'rgba(255, 255, 255, 0.4)',
                cursor: 'pointer',
                padding: '2px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: '50%',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.currentTarget.style.color = '#fff'}
              onMouseLeave={(e) => e.currentTarget.style.color = 'rgba(255, 255, 255, 0.4)'}
            >
              <XCircle size={14} />
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
