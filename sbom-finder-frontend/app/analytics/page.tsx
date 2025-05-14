"use client";

import { useEffect, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell
} from "recharts";
import "./analytics.css";

// Tooltip component to show detailed info for suppliers
const SupplierTooltip = ({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: SupplierData }[];
}) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p><strong>{data.supplier}</strong></p>
        <p><strong>Packages:</strong> {data.packageCount}</p>
        <ul>
          {data.packages?.slice(0, 5).map((pkg, idx) => (
            <li key={idx}>{pkg.name} ({pkg.version})</li>
          ))}
        </ul>
        {data.packages?.length > 5 && <p>+ more...</p>}
      </div>
    );
  }
  return null;
};

// Type definitions
type AnalyticsTab = "category" | "operatingSystem" | "supplier" | "manufacturer" | "vulnerabilities";

type SupplierData = {
  supplier: string;
  packageCount: number;
  packages: { name: string; version: string }[];
};

type VulnerableSupplier = {
  name: string;
  vulns: number;
};

export default function AnalyticsPage() {
  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

  // UI state management
  const [selected, setSelected] = useState<AnalyticsTab>("category");
  const [analyticsData, setAnalyticsData] = useState<Record<AnalyticsTab, unknown[]>>({
    category: [],
    operatingSystem: [],
    supplier: [],
    manufacturer: [],
    vulnerabilities: [],
  });

  // Chart-specific states
  const [vulnerabilityCategoryData, setVulnerabilityCategoryData] = useState([]);
  const [severityData, setSeverityData] = useState([]);
  const [topVulnerablePackages, setTopVulnerablePackages] = useState([]);
  const [vulnerableSuppliers, setVulnerableSuppliers] = useState<VulnerableSupplier[]>([]);
  const [loading, setLoading] = useState(true);

  // Fetch data on page load
  useEffect(() => {
    const fetchData = async () => {
      try {
        const endpoints = [
          "category", "operating-systems", "suppliers", "manufacturers",
          "vulnerabilities-by-category", "top-vulnerable-packages",
          "vulnerability-severity", "vulnerable-suppliers"
        ];

        const responses = await Promise.all(
          endpoints.map(endpoint => fetch(`${BASE_URL}/api/analytics/${endpoint}`))
        );

        const [
          category, operatingSystem, supplier, manufacturer,
          vulnCategory, topPackages, severity, vulnSuppliers
        ] = await Promise.all(responses.map(res => res.json()));

        setAnalyticsData({
          category,
          operatingSystem,
          supplier: supplier.map((s: SupplierData) => ({
            name: s.supplier,
            packageCount: s.packageCount,
            packages: s.packages,
          })),
          manufacturer,
          vulnerabilities: [],
        });

        setVulnerabilityCategoryData(vulnCategory);
        setTopVulnerablePackages(topPackages);
        setSeverityData(severity);
        setVulnerableSuppliers(vulnSuppliers.filter(s => s.vulns > 0));
      } catch (error) {
        console.error("Error fetching analytics data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading analytics...</p>
      </div>
    );
  }

  return (
    <div className="analytics-container">
      <h1 className="analytics-page-title">SBOM Analytics</h1>

      {/* Tab buttons */}
      <div className="analytics-tabs">
        {["category", "operatingSystem", "supplier", "manufacturer", "vulnerabilities"].map(tab => (
          <button
            key={tab}
            className={selected === tab ? "active" : ""}
            onClick={() => setSelected(tab as AnalyticsTab)}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {/* Dynamic chart rendering based on selected tab */}
      {selected === "supplier" ? (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={analyticsData.supplier}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" angle={-45} textAnchor="end" />
            <YAxis allowDecimals={false} />
            <Tooltip content={<SupplierTooltip />} />
            <Legend />
            <Bar dataKey="packageCount" fill="#8884d8" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      ) : selected !== "vulnerabilities" ? (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={analyticsData[selected]}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Legend />
            <Bar dataKey="sboms" fill="#8884d8" />
          </BarChart>
        </ResponsiveContainer>
      ) : (
        <div className="vulnerability-analytics">
          {/* Vulnerability Category Pie Chart */}
          <h2>Most Vulnerable Category</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie data={vulnerabilityCategoryData} dataKey="value" nameKey="name" outerRadius={100} label>
                {vulnerabilityCategoryData.map((_, index) => (
                  <Cell key={index} fill={["#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF"][index % 4]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>

          {/* Top Vulnerable Packages */}
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

          {/* Affected Suppliers */}
          <h2>Affected Suppliers</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={vulnerableSuppliers}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" angle={-45} textAnchor="end" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="vulns" fill="#FF8A65" />
            </BarChart>
          </ResponsiveContainer>

          {/* Severity Breakdown */}
          <h2>Severity Breakdown</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={severityData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="value" fill="#8884d8" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
