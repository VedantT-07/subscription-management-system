// LOGIN
document.getElementById("loginForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();

  const payload = {
    email: document.getElementById("email").value,
    password: document.getElementById("password").value
  };

  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  const text = await res.text();
  if (!res.ok) {
    alert("Login failed: " + text);
    return;
  }

  const user = JSON.parse(text);
  // store user id & email for later (simple dev approach)
  localStorage.setItem("userId", user.id);
  localStorage.setItem("email", user.email)
  window.location.href = "dashboard.html";
});

// REGISTER
document.getElementById("registerForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();

  const payload = {
    name: document.getElementById("name").value,
    email: document.getElementById("regEmail").value,
    password: document.getElementById("regPassword").value
  };

  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  const text = await res.text();
  if (!res.ok) {
    alert("Register failed: " + text);
    return;
  }

  alert("Registered successfully!");
  window.location.href = "login.html";
});


// ADD SUBSCRIPTION
document.getElementById("subscriptionForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();

  const email = localStorage.getItem("email");
  if (!email) {
    alert("Please login first");
    window.location.href = "login.html";
    return;
  }

  const payload = {
    email: email,
    serviceName: document.getElementById("serviceName").value,
    planType: document.getElementById("planType").value,
    startDate: document.getElementById("startDate").value,
    amount: Number(document.getElementById("amount").value),
    category: document.getElementById("category").value
  };

  const res = await fetch("/api/subscriptions", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  const text = await res.text();
  if (!res.ok) {
    alert("Add failed: " + text);
    return;
  }

  alert("Subscription added!");
  window.location.href = "subscriptions.html";
});


// SUBSCRIPTIONS TABLE
function renderSubscriptions(subs) {
  const table = document.getElementById("subscriptionTable");

  if (!subs || !subs.length) {
    table.innerHTML = `<tr><td colspan="7" style="text-align:center">No subscriptions found</td></tr>`;
    return;
  }

  table.innerHTML = subs.map(s => `
    <tr>
      <td>${s.serviceName}</td>
      <td>${s.planType}</td>
      <td>${s.renewalDate}</td>
      <td>${s.category}</td>
      <td>₹${s.amount} / ${s.planType === "MONTHLY" ? "month" : "year"}</td>
      <td>${s.status}</td>
      <td>
        ${s.status === "ACTIVE"
          ? `<button onclick="cancelSub(${s.id})">Cancel</button>`
          : s.status === "EXPIRED"
          ? `<button onclick="renewSub(${s.id})">Renew</button>`
          : `<span style="opacity:0.6">—</span>`
        }
      </td>
    </tr>
  `).join("");
}


async function loadSubscriptions() {

  const table = document.getElementById("subscriptionTable");
  if (!table) return;

  const email = localStorage.getItem("email");

  if (!email) {
    alert("Please login first");
    window.location.href = "login.html";
    return;
  }

  const res = await fetch(`/api/subscriptions/my?email=${email}`);

  if (!res.ok) {
    alert("Failed to load subscriptions");
    return;
  }

  const subs = await res.json();

  renderSubscriptions(subs);
}


async function cancelSub(id) {

  if (!confirm("Cancel this subscription?")) return;

  const res = await fetch(`/api/subscriptions/${id}/cancel`, {
    method: "PUT"
  });

  if (!res.ok) {
    const txt = await res.text();
    alert("Cancel failed: " + txt);
    return;
  }

  await loadSubscriptions();
}


async function renewSub(id) {

  if (!confirm("Renew this subscription?")) return;

  const res = await fetch(`/api/subscriptions/${id}/renew`, {
    method: "PUT"
  });

  if (!res.ok) {
    const txt = await res.text();
    alert("Renew failed: " + txt);
    return;
  }

  await loadSubscriptions();
}


async function searchSubscriptions() {

  const email = localStorage.getItem("email");

  if (!email) return;

  const search = document.getElementById("searchInput")?.value || "";
  const category = document.getElementById("categoryFilter")?.value || "";
  const status = document.getElementById("statusFilter")?.value || "";
  const sort = document.getElementById("sortOption")?.value || "";

  let url = `/api/subscriptions/my/search?email=${email}`;

  if (search) url += `&search=${encodeURIComponent(search)}`;
  if (category) url += `&category=${category}`;
  if (status) url += `&status=${status}`;
  if (sort) url += `&sort=${sort}`;

  const res = await fetch(url);

  if (!res.ok) {
    alert("Search failed");
    return;
  }

  const subs = await res.json();

  renderSubscriptions(subs);
}


document.addEventListener("DOMContentLoaded", () => {

  loadSubscriptions();

  document.getElementById("searchInput")?.addEventListener("input", searchSubscriptions);

  document.getElementById("categoryFilter")?.addEventListener("change", searchSubscriptions);

  document.getElementById("statusFilter")?.addEventListener("change", searchSubscriptions);

  document.getElementById("sortOption")?.addEventListener("change", searchSubscriptions);

});
// DASHBOARD
async function loadDashboard() {
    const email = localStorage.getItem("email");
    if(!email) return;

    //Load stats
    const statsRes = await fetch(`/api/subscriptions/my/stats?email=${email}`);

    if (statsRes.ok){
        const stats = await statsRes.json();
        document.getElementById("activeCount").innerText = stats.activeCount;
        document.getElementById("upcomingCount").innerText = stats.expiringSoonCount;
        document.getElementById("expiredCount").innerText = stats.expiredCount;
        document.getElementById("monthlySpend").innerText = stats.monthlySpend;
    }

    //Load expiring soon list
    const expRes = await fetch(`/api/subscriptions/my/expiring?email=${email}`);

    function daysLeft(dateStr) {
      const [y, m, d] = dateStr.split("-").map(Number);
      const target = new Date(y, m - 1, d);
      const today = new Date();
      today.setHours(0,0,0,0);

      const diffMs = target - today;
      const days = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
      return days >= 0 ? days : 0;
    }

    if(expRes.ok)
    {
        const subs = await expRes.json();
        const tbody = document.getElementById("upcomingTable");
        tbody.innerHTML = subs.length
        ? subs.map(s => `
            <tr>
                <td>${s.serviceName}</td>
                <td>${s.renewalDate}</td>
                <td>${daysLeft(s.renewalDate)}</td>
            </tr>
          `).join("")
        : `<tr><td colspan="3" style="text-align:center">No upcoming renewals</td></tr>`;
    }

    //Load expired table
    function daysSince(dateStr) {
      const [y, m, d] = dateStr.split("-").map(Number);
      const target = new Date(y, m - 1, d);
      const today = new Date();
      today.setHours(0,0,0,0);

      const diff = Math.ceil((today - target) / (1000 * 60 * 60 * 24));
      return diff >= 0 ? diff : 0;
    }

    const expiredRes = await fetch(`/api/subscriptions/my/expired/recent?email=${email}`);
    if (expiredRes.ok) {
      const subs = await expiredRes.json();
      const expiredTbody = document.getElementById("expiredTable");
      expiredTbody.innerHTML = subs.length
        ? subs.map(s => `
            <tr>
              <td>${s.serviceName}</td>
              <td>${s.renewalDate}</td>
              <td>${daysSince(s.renewalDate)}</td>
            </tr>
          `).join("")
        : `<tr><td colspan="3" style="text-align:center">No recently expired subscriptions</td></tr>`;
    }

    const expensiveRes = await fetch(`/api/subscriptions/my/most-expensive?email=${email}`);
    if (expensiveRes.ok) {
      const sub = await expensiveRes.json();
      if (sub) {
        document.getElementById("mostExpensiveService").innerText = sub.serviceName;
        document.getElementById("mostExpensiveAmount").innerText = sub.amount;
      } else {
        document.getElementById("mostExpensiveService").innerText = "No active subscriptions";
        document.getElementById("mostExpensiveAmount").innerText = "0";
      }
    }

    const catRes = await fetch(`/api/subscriptions/my/category-spend?email=${email}`);

    if (catRes.ok) {
      const data = await catRes.json();
      const tbody = document.getElementById("categorySpendTable");

      if (!Object.keys(data).length) {
        tbody.innerHTML = `<tr><td colspan="2">No active subscriptions</td></tr>`;
      } else {
        tbody.innerHTML = Object.entries(data).map(([category, amount]) => `
          <tr>
            <td>${category}</td>
            <td>₹${amount}</td>
          </tr>
        `).join("");
      }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    if (document.getElementById("activeCount")) {
        loadDashboard();
    }
});


function logout() {
    localStorage.removeItem("email");
    window.location.href = "login.html";
}