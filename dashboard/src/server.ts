import express from "express";
import orderRouter from "../src/routes/orderRouter"
import getTrades from "../src/routes/getTrades"

const app = express()

app.use(express.json)

app.use(orderRouter)
app.use(getTrades)

app.listen(3000, ()=>{
    console.log("Server Live on 3000")
})
