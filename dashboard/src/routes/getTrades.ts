import express from "express";
import { allTrades } from "../stores/trade";

const router = express.Router();

router.get("/trades", (req, res) => {
    res.json(allTrades);
});

export default router;