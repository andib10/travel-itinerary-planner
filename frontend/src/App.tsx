import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import VerifyEmailPage from './pages/VerifyEmailPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import MyTripsPage from './pages/MyTripsPage'
import AccountPage from './pages/AccountPage'
import TripRequestPage from './pages/TripRequestPage'
import ItineraryDetailPage from './pages/ItineraryDetailPage'
import AdminDashboardPage from './pages/AdminDashboardPage'
import AdminIngestionPage from './pages/AdminIngestionPage'
import AdminPoisPage from './pages/AdminPoisPage'
import AdminUsersPage from './pages/AdminUsersPage'
import AdminTripsPage from './pages/AdminTripsPage'
import AdminTripDetailPage from './pages/AdminTripDetailPage'

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route
            path="/trips"
            element={
              <ProtectedRoute>
                <MyTripsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/account"
            element={
              <ProtectedRoute>
                <AccountPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/trips/new"
            element={
              <ProtectedRoute>
                <TripRequestPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/trips/:id"
            element={
              <ProtectedRoute>
                <ItineraryDetailPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboardPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/ingestion"
            element={
              <AdminRoute>
                <AdminIngestionPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/pois"
            element={
              <AdminRoute>
                <AdminPoisPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <AdminRoute>
                <AdminUsersPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/trips"
            element={
              <AdminRoute>
                <AdminTripsPage />
              </AdminRoute>
            }
          />
          <Route
            path="/admin/trips/:id"
            element={
              <AdminRoute>
                <AdminTripDetailPage />
              </AdminRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}

export default App
