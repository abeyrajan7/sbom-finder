"use client";

import SideNavBar from "./SideNavBar";

export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: "flex" }}>
      <SideNavBar />
      <main style={{ flex: 1, padding: "0rem", marginLeft: "12vw" }}>
      {children}
      </main>
    </div>
  );
}

