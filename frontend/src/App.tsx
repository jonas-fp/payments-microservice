import { useEffect, useState } from 'react'
import './App.css'

function App() {
  const [message, setMessage] = useState<string>('Loading...')

  useEffect(() => {
    fetch('/api/hello')
      .then((res) => res.text())
      .then((data) => setMessage(data))
      .catch((err) => {
        console.error('Error fetching message:', err)
        setMessage('Error connecting to backend')
      })
  }, [])

  return (
    <div className="dashboard-container">
      <h1>Payments Dashboard</h1>
      <p>Backend Message: <strong>{message}</strong></p>
    </div>
  )
}

export default App
