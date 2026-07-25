import { useState, type ChangeEvent } from "react";
import { sendOrder } from "../api/v1/Order";

interface SymbolOption {
  value: string;
  label: string;
}

const SYMBOLS: SymbolOption[] = [
  { value: "AAPL", label: "AAPL - Apple Inc." },
  { value: "MSFT", label: "MSFT - Microsoft Corp." },
  { value: "GOOGL", label: "GOOGL - Alphabet Inc." },
];

function LandingDashboard() {
  const [symbol, setSymbol] = useState<string>("AAPL");
  const [quantity, setQuantity] = useState<number>(1);
  const [price, setPrice] = useState<number>(1);

  function handleSymbolChange(e: ChangeEvent<HTMLSelectElement>) {
    setSymbol(e.target.value);
  }

  function handleQuantityChange(e: ChangeEvent<HTMLInputElement>) {
    setQuantity(parseInt(e.target.value));
  }

  function handlePriceChange(e: ChangeEvent<HTMLInputElement>) {
    setPrice(parseInt(e.target.value));
  }

  return (
    <div className="flex justify-center align-middle">
      <div className="w-full max-w-sm bg-white rounded-2xl border border-gray-200 shadow-sm p-6">
        <h1 className="text-lg font-medium text-gray-900 mb-5">
          Place an order
        </h1>
        <div className="mb-4">
          <label
            htmlFor="symbol"
            className="block text-sm text-gray-600 mb-1.5"
          >
            Symbol
          </label>
          <select
            id="symbol"
            value={symbol}
            onChange={handleSymbolChange}
            className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            {SYMBOLS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
          <div className="flex">
            <div className="mb-4 mt-6 mr-4">
              <label
                htmlFor="quantity"
                className="block text-sm text-gray-600 mb-1.5"
              >
                Price in Rupees
              </label>

              <input
                id="price"
                type="number"
                min="1"
                step="1"
                value={price}
                onChange={handlePriceChange}
                placeholder="Cost of one shares"
                className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <div className="mb-4 mt-14">X</div>
            <div className="mb-4 mt-6 ml-4">
              <label
                htmlFor="quantity"
                className="block text-sm text-gray-600 mb-1.5"
              >
                Quantity
              </label>
              <input
                id="quantity"
                type="number"
                min="1"
                step="1"
                value={quantity}
                onChange={handleQuantityChange}
                placeholder="Number of shares"
                className="w-full h-10 rounded-lg border border-gray-300 px-3 text-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
          {quantity > 1 || price > 1 ? (
            <div className="m-2 pb-4">
              <h4>Total: ₹ {quantity * price}</h4>
            </div>
          ) : null}
          <div>
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => sendOrder({symbol, quantity, price, side: "BUY", type: "LIMIT"})}
                className="h-10 rounded-lg bg-green-600 text-white text-sm font-medium hover:bg-green-700 active:scale-[0.98] transition"
              >
                Buy
              </button>
              <button
                onClick={() => sendOrder({
                  symbol, quantity, price, side: "SELL",
                  type: "LIMIT"
                })}
                className="h-10 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 active:scale-[0.98] transition"
              >
                Sell
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default LandingDashboard;
