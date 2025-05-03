"use client";

import { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import "./analytics.css";

interface SupplierData {
  supplier: string;
  packageCount: number;
  packages: { name: string; version: string }[];
}

interface VulnerableSupplier {
  name: string;
  vulns: number;
}

interface PayloadItem<T> {
  payload: T;
}

const SupplierTooltip = ({ active, payload }: { active?: boolean; payload?: PayloadItem<SupplierData>[] }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p><strong>{data.supplier}</strong></p>
        <p style={{ marginBottom: "0.25rem" }}><strong>Packages:</strong> {data.packageCount}</p>
        <ul style={{ paddingLeft: "1rem", margin: 0 }}>
          {data.packages?.slice(0, 5).map((pkg, idx) => (
            <li key={idx} style={{ fontSize: "0.8rem" }}>
              {pkg.name} ({pkg.version})
            </li>
          ))}
        </ul>
        {data.packages?.length > 5 && <p style={{ fontSize: "0.75rem", color: "#999" }}>+ more...</p>}
      </div>
    );
  }
  return null;
};

const DefaultTooltip = ({ active, payload }: { active?: boolean; payload?: PayloadItem<{ name: string; devices: number }>[] }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p><strong>{data.name}</strong></p>
        <p>Device Count: {data.devices}</p>
      </div>
    );
  }
  return null;
};

const PackageTooltip = ({ active, payload }: { active?: boolean; payload?: PayloadItem<{ name: string; vulns: number }>[] }) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p><strong>{data.name}</strong></p>
        <p>Vulns: {data.vulns}</p>
      </div>
    );
  }
  return null;
};
