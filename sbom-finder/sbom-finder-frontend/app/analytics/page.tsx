"use client";

import { useState } from "react";
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
type AnalyticsTab =
  | "category"
  | "operatingSystem"
  | "supplier"
  | "manufacturer"
  | "vulnerabilities";

const dummyData: Record<AnalyticsTab, { name: string; sboms: number }[]> = {
  category: [
    { name: "Fitness Wearables", sboms: 10 },
    { name: "Smart Home", sboms: 7 },
  ],
  operatingSystem: [
    { name: "Android", sboms: 12 },
    { name: "Fitbit OS", sboms: 5 },
    { name: "Tizen", sboms: 3 },
  ],
  supplier: [
    { name: "Google", sboms: 10 },
    { name: "Samsung", sboms: 8 },
    { name: "Fitbit", sboms: 2 },
  ],
  manufacturer: [
    { name: "Samsung", sboms: 9 },
    { name: "Fitbit", sboms: 5 },
    { name: "Sony", sboms: 3 },
  ],
  vulnerabilities: [],
};

const vulnerabilityCategoryData = [
  { name: "Smart Home", value: 60 },
  { name: "Fitness Wearables", value: 40 },
];

const severityData = [
  { name: "Critical", value: 12 },
  { name: "High", value: 22 },
  { name: "Medium", value: 30 },
  { name: "Low", value: 18 },
  { name: "Unknown", value: 8 },
];

const topVulnerablePackages = [
  { name: "openssl", vulns: 15 },
  { name: "glibc", vulns: 12 },
  { name: "nginx", vulns: 10 },
  { name: "libxml", vulns: 9 },
  { name: "sqlite", vulns: 8 },
  { name: "curl", vulns: 7 },
  { name: "zlib", vulns: 7 },
  { name: "bash", vulns: 6 },
  { name: "python", vulns: 6 },
  { name: "busybox", vulns: 5 },
];

const vulnerableSuppliers = [
  { name: "Google", vulns: 25 },
  { name: "Samsung", vulns: 15 },
  { name: "Open Source", vulns: 18 },
  { name: "Fitbit", vulns: 10 },
];

// const vulnSeverityData = [
//   { name: "Critical", value: 14 },
//   { name: "High", value: 24 },
//   { name: "Medium", value: 30 },
//   { name: "Low", value: 18 },
//   { name: "Unknown", value: 6 },
// ];

// const severityColors: { [key: string]: string } = {
//   Critical: "#d32f2f",
//   High: "#f44336",
//   Medium: "#ff9800",
//   Low: "#ffeb3b",
//   Unknown: "#9e9e9e",
// };

// const sections = [
//   "category",
//   "operatingSystem",
//   "supplier",
//   "manufacturer",
//   "vulnerabilities",
// ];

export default function AnalyticsPage() {
  const [selected, setSelected] = useState<AnalyticsTab>("category");
  return (
    <div className="analytics-container">
      <h1>SBOM Analytics</h1>

      <div className="analytics-tabs">
        {[
          "category",
          "operatingSystem",
          "supplier",
          "manufacturer",
          "vulnerabilities",
        ].map((tab) => (
          <button
            key={tab}
            className={selected === tab ? "active" : ""}
            onClick={() => setSelected(tab as AnalyticsTab)}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>
      {selected != "vulnerabilities" ? (
        <div className="chart-container">
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={dummyData[selected]}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="sboms" fill="#8884d8" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="vulnerability-analytics">
          <div className="chart-block">
            <h2>Most Vulnerable Category</h2>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={vulnerabilityCategoryData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius={100}
                  label
                >
                  {vulnerabilityCategoryData.map((entry, index) => (
                    <Cell
                      key={index}
                      fill={
                        ["#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF"][index % 4]
                      }
                    />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-block">
            <h2>Top 10 Vulnerable Packages</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topVulnerablePackages}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="vulns" fill="#8884d8" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-block">
            <h2>Affected Suppliers</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={vulnerableSuppliers}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="vulns" fill="#FF8A65" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-block">
            <h2>Severity Breakdown</h2>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={severityData}
                  dataKey="value"
                  nameKey="name"
                  outerRadius={100}
                  label
                >
                  {severityData.map((entry, index) => (
                    <Cell
                      key={index}
                      fill={
                        {
                          Critical: "#d32f2f",
                          High: "#f44336",
                          Medium: "#ff9800",
                          Low: "#cddc39",
                          Unknown: "#9e9e9e",
                        }[entry.name]
                      }
                    />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}
