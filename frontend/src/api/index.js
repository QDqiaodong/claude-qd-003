import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const greenhouseApi = {
  list: () => http.get('/greenhouses'),
  create: (data) => http.post('/greenhouses', data),
  setStatus: (id, status) =>
    http.put(`/greenhouses/${id}/status?status=${encodeURIComponent(status)}`)
}

export const seedbedApi = {
  list: (params) => http.get('/seedbeds', { params }),
  create: (data) => http.post('/seedbeds', data),
  update: (id, data) => http.put(`/seedbeds/${id}`, data)
}

export const varietyApi = {
  list: () => http.get('/varieties'),
  create: (data) => http.post('/varieties', data),
  setStatus: (id, status) => http.put(`/varieties/${id}/status?status=${encodeURIComponent(status)}`)
}

export const batchApi = {
  list: (params) => http.get('/batches', { params }),
  open: (data) => http.post('/batches', data),
  advance: (id, action, actualQty) =>
    http.post(`/batches/${id}/advance`, null, { params: { action, actualQty } })
}

export const transferApi = {
  list: (params) => http.get('/transfers', { params }),
  post: (data) => http.post('/transfers', data)
}

export const occupancyApi = {
  list: (params) => http.get('/occupancies', { params })
}

export const shipmentApi = {
  list: (params) => http.get('/shipments', { params }),
  open: (data) => http.post('/shipments', data),
  advance: (id, action, carrier, shipDate) =>
    http.post(`/shipments/${id}/advance`, null, { params: { action, carrier, shipDate } })
}

export default http
