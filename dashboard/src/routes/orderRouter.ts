import express from "express";
import { sendOrder } from "../api/v1/order";
import { allTrades } from "../stores/trade";

const router = express.Router();

router.post("/placeOrder", async (req, res) => {
    try {
        const trades = await sendOrder(req.body);

        allTrades.push(...trades);

        res.json(trades);
    } catch (err) {
        res.status(500).json({ error: "Failed to place order" });
    }
});

export default router;