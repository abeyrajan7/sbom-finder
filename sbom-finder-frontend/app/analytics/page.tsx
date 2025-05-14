"use client";

import { useEffect, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell
} from "recharts";
import "./analytics.css";

/* === Type Definitions === */
type AnalyticsTab = "category" | "operatingSystem" | "supplier" | "manufacturer" | "vulnerabilities";

type BasicSbomCount = { name: string; sboms: number };
type SupplierData = {
  supplier: string;
  packageCount: number;
  packages: { name: string; version: string }[];
};
type VulnerabilityStat = { name: string; value: number };
type TopVulnerablePackage = { name: string; vulns: number };
type VulnerableSupplier = { name: string; vulns: number };

type AnalyticsDataMap = {
  category: BasicSbomCount[];
  operatingSystem: BasicSbomCount[];
  manufacturer: BasicSbomCount[];
  supplier: SupplierData[];
  vulnerabilities: never[];
};

/* === Tooltip for Supplier Chart === */
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

export default function AnalyticsPage() {
  const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const [selected, setSelected] = useState<AnalyticsTab>("category");

  const [analyticsData, setAnalyticsData] = useState<AnalyticsDataMap>({
    category: [],
    operatingSystem: [],
    manufacturer: [],
    supplier: [],
    vulnerabilities: [],
  });

  const [vulnerabilityCategoryData, setVulnerabilityCategoryData] = useState<VulnerabilityStat[]>([]);
  const [severityData, setSeverityData] = useState<VulnerabilityStat[]>([]);
  const [topVulnerablePackages, setTopVulnerablePackages] = useState<TopVulnerablePackage[]>([]);
  const [vulnerableSuppliers, setVulnerableSuppliers] = useState<VulnerableSupplier[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [
          categoryRes, osRes, supplierRes, manufacturerRes,
          vulnCategoryRes, topPackagesRes, severityRes, vulnSuppliersRes
        ] = await Promise.all([
          fetch(`${BASE_URL}/api/analytics/category`),
          fetch(`${BASE_URL}/api/analytics/operating-systems`),
          fetch(`${BASE_URL}/api/analytics/suppliers`),
          fetch(`${BASE_URL}/api/analytics/manufacturers`),
          fetch(`${BASE_URL}/api/analytics/vulnerabilities-by-category`),
          fetch(`${BASE_URL}/api/analytics/top-vulnerable-packages`),
          fetch(`${BASE_URL}/api/analytics/vulnerability-severity`),
          fetch(`${BASE_URL}/api/analytics/vulnerable-suppliers`)
        ]);

        const category = await categoryRes.json() as BasicSbomCount[];
        const operatingSystem = await osRes.json() as BasicSbomCount[];
        const supplier = await supplierRes.json() as SupplierData[];
        const manufacturer = await manufacturerRes.json() as BasicSbomCount[];
        const vulnCategory = await vulnCategoryRes.json() as VulnerabilityStat[];
        const topPackages = await topPackagesRes.json() as TopVulnerablePackage[];
        const severity = await severityRes.json() as VulnerabilityStat[];
        const vulnSuppliers = await vulnSuppliersRes.json() as VulnerableSupplier[];

        setAnalyticsData({
          category,
          operatingSystem,
          manufacturer,
          supplier,
          vulnerabilities: [],
        });

        setVulnerabilityCategoryData(vulnCategory);
        setTopVulnerablePackages(topPackages);
        setSeverityData(severity);
        setVulnerableSuppliers(vulnSuppliers.filter(s => s.vulns > 0));
      } catch (error) {
        console.error("Error loading analytics:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [BASE_URL]);

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

      {/* === Supplier Tab === */}
      {selected === "supplier" && (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={analyticsData.supplier}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="supplier" angle={-45} textAnchor="end" />
            <YAxis />
            <Tooltip content={<SupplierTooltip />} />
            <Legend />
            <Bar dataKey="packageCount" fill="#8884d8" />
          </BarChart>
        </ResponsiveContainer>
      )}

      {/* === Category, OS, Manufacturer Tabs === */}
      {["category", "operatingSystem", "manufacturer"].includes(selected) && (
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={analyticsData[selected]}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="sboms" fill="#8884d8" />
          </BarChart>
        </ResponsiveContainer>
      )}

      {/* === Vulnerabilities Tab === */}
      {selected === "vulnerabilities" && (
        <div className="vulnerability-analytics">
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
