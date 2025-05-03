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

const SupplierTooltip = ({ active, payload }: any) => {
  if (active && payload && payload.length) {
    const data = payload[0].payload;
    return (
      <div className="custom-tooltip">
        <p><strong>{data.name}</strong></p>
        <p style={{ marginBottom: "0.25rem" }}><strong>Packages:</strong> {data.packageCount}</p>
        <ul style={{ paddingLeft: "1rem", margin: 0 }}>
          {data.packages?.slice(0, 5).map((pkg: any, idx: number) => (
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

const DefaultTooltip = ({ active, payload }: any) => {
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

const PackageTooltip = ({ active, payload }: any) => {
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

type AnalyticsTab =
  | "category"
  | "operatingSystem"
  | "supplier"
  | "manufacturer"
  | "vulnerabilities";

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
  const [selected, setSelected] = useState<AnalyticsTab>("category");
  const [analyticsData, setAnalyticsData] = useState<Record<AnalyticsTab, { name: string; devices: number; }[]>>({
    category: [],
    operatingSystem: [],
    supplier: [],
    manufacturer: [],
    vulnerabilities: [],
  });
  const [supplierData, setSupplierData] = useState<SupplierData[]>([]);
  const [vulnerabilityCategoryData, setVulnerabilityCategoryData] = useState<{ name: string; value: number }[]>([]);
  const [severityData, setSeverityData] = useState<{ name: string; value: number }[]>([]);
  const [topVulnerablePackages, setTopVulnerablePackages] = useState<{ name: string; vulns: number }[]>([]);
  const [vulnerableSuppliers, setVulnerableSuppliers] = useState<VulnerableSupplier[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [
          categoryRes,
          osRes,
          supplierRes,
          manufacturerRes,
          vulnCategoryRes,
          topPackagesRes,
          severityRes,
          vulnSuppliersRes,
        ] = await Promise.all([
          fetch(`${BASE_URL}/api/analytics/category`),
          fetch(`${BASE_URL}/api/analytics/operating-systems`),
          fetch(`${BASE_URL}/api/analytics/suppliers`),
          fetch(`${BASE_URL}/api/analytics/manufacturers`),
          fetch(`${BASE_URL}/api/analytics/vulnerabilities-by-category`),
          fetch(`${BASE_URL}/api/analytics/top-vulnerable-packages`),
          fetch(`${BASE_URL}/api/analytics/vulnerability-severity`),
          fetch(`${BASE_URL}/api/analytics/vulnerable-suppliers`),
        ]);

        const [
          category,
          operatingSystem,
          supplier,
          manufacturer,
          vulnCategory,
          topPackages,
          severity,
          vulnSuppliers,
        ] = await Promise.all([
          categoryRes.json(),
          osRes.json(),
          supplierRes.json(),
          manufacturerRes.json(),
          vulnCategoryRes.json(),
          topPackagesRes.json(),
          severityRes.json(),
          vulnSuppliersRes.json(),
        ]);

        setAnalyticsData({
          category: category.map((d: any) => ({ ...d, devices: d.sboms })),
          operatingSystem: operatingSystem.map((d: any) => ({ ...d, devices: d.sboms })),
          manufacturer: manufacturer.map((d: any) => ({ ...d, devices: d.sboms })),
          supplier: [],
          vulnerabilities: [],
        });

        setSupplierData(supplier);
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
      <h1>SBOM Analytics</h1>

      <div className="analytics-tabs">
        {["category", "operatingSystem", "supplier", "manufacturer", "vulnerabilities"].map((tab) => (
          <button
            key={tab}
            className={selected === tab ? "active" : ""}
            onClick={() => setSelected(tab as AnalyticsTab)}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {selected === "supplier" ? (
        <div className="chart-container">
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={supplierData.map(s => ({ name: s.supplier, packageCount: s.packageCount, packages: s.packages }))} margin={{ top: 20, bottom: 60 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" />
              <YAxis allowDecimals={false} />
              <Tooltip content={<SupplierTooltip />} />
              <Legend />
              <Bar dataKey="packageCount" fill="#8884d8" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : selected !== "vulnerabilities" ? (
        <div className="chart-container">
          <ResponsiveContainer width="100%" height={400}>
            <BarChart data={analyticsData[selected]} margin={{ top: 20, bottom: 60 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" />
              <YAxis allowDecimals={false} label={{ value: "Device Count", angle: -90, position: "insideLeft" }} />
              <Tooltip content={<DefaultTooltip />} />
              <Legend />
              <Bar dataKey="devices" fill="#8884d8" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="vulnerability-analytics">
          <div className="chart-block">
            <h2>Most Vulnerable Category</h2>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={vulnerabilityCategoryData} dataKey="value" nameKey="name" outerRadius={100} label>
                  {vulnerabilityCategoryData.map((entry, index) => (
                    <Cell
                      key={index}
                      fill={["#FF6B6B", "#FFD93D", "#6BCB77", "#4D96FF"][index % 4]}
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
              <BarChart data={topVulnerablePackages} margin={{ top: 20, bottom: 60 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" />
                <YAxis />
                <Tooltip content={<PackageTooltip />} />
                <Legend />
                <Bar dataKey="vulns" fill="#8884d8" />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-block">
            <h2>Affected Suppliers</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={vulnerableSuppliers} margin={{ top: 20, bottom: 60 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" interval={0} angle={-45} textAnchor="end" />
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
        </div>
      )}
    </div>
  );
}
