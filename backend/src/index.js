import express from 'express'
import { config } from './config.js'
import { callbackRouter } from './callback.js'
import { router } from './routes.js'

const app = express()

app.use(callbackRouter)
app.use(express.json())
app.use(router)

app.use((error, _request, response, _next) => {
  console.error(error)
  response.status(error.status ?? 500).json({ error: error.message })
})

app.listen(config.port, config.host, () =>
  console.log(`Listening on ${config.host}:${config.port}`),
)
