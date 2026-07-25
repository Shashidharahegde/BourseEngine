export interface OrderRequest{
    symbol: string;
    side: "BUY" | "SELL"
    type: "LIMIT" | "MARKET"
    price: number
    quantity: number;
}