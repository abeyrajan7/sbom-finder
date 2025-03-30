"use client"; // ❗Only use if you need hooks like useRouter()

import SideNavBar from "./SideNavBar";

export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ display: "flex" }}>
      <SideNavBar />
      <main style={{ flex: 1, padding: "1rem" }}>{children}</main>
    </div>
  );
}
