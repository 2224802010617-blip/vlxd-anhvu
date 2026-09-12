document.addEventListener("DOMContentLoaded", () => {
    const data = window.adminChartData || {};
    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    const COLORS = ["#f43f5e", "#f59e0b", "#38bdf8", "#22c55e", "#a78bfa", "#fb923c", "#94a3b8", "#2dd4bf"];
    const STATUS_NAMES = { NEW: "Mới", CONFIRMED: "Đã xác nhận", SHIPPING: "Đang giao", COMPLETED: "Hoàn thành", CANCELED: "Đã hủy" };
    const STATUS_COLORS = { NEW: "#f43f5e", CONFIRMED: "#f59e0b", SHIPPING: "#38bdf8", COMPLETED: "#22c55e", CANCELED: "#94a3b8" };

    // Rut gon so lon: 1500000 -> "1,5M", 42000 -> "42K"
    const shortNumber = (value) => {
        if (value >= 1000000000) {
            return (value / 1000000000).toFixed(value % 1000000000 === 0 ? 0 : 1).replace(".", ",") + "B";
        }
        if (value >= 1000000) {
            return (value / 1000000).toFixed(value % 1000000 === 0 ? 0 : 1).replace(".", ",") + "M";
        }
        if (value >= 1000) {
            return (value / 1000).toFixed(value % 1000 === 0 ? 0 : 1).replace(".", ",") + "K";
        }
        return Math.round(value).toString();
    };
    const fullNumber = (value) => Math.round(value).toLocaleString("vi-VN");

    // ----- Dem so: doc gia tri goc tu data-value (server da dinh dang san text neu JS khong chay) -----
    const countElements = Array.from(document.querySelectorAll("[data-countup]"));
    countElements.forEach((el, index) => {
        const target = Number(el.getAttribute("data-value")) || 0;
        const suffix = el.getAttribute("data-suffix") || "";
        el.style.setProperty("--count-delay", `${index * 90}ms`);
        if (prefersReducedMotion) {
            el.textContent = fullNumber(target) + suffix;
            return;
        }
        const duration = 1000;
        const start = performance.now();
        const tick = (now) => {
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            el.textContent = fullNumber(target * eased) + suffix;
            if (progress < 1) {
                requestAnimationFrame(tick);
            }
        };
        requestAnimationFrame(tick);
    });

    // ----- Chuan bi canvas theo kich thuoc CSS + retina -----
    const prepare = (canvas, ratio) => {
        const ctx = canvas.getContext("2d");
        const dpr = window.devicePixelRatio || 1;
        const width = Math.max(240, canvas.clientWidth || 560);
        const height = Math.max(240, Math.round(width * ratio));
        canvas.width = Math.round(width * dpr);
        canvas.height = Math.round(height * dpr);
        canvas.style.height = height + "px";
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, width, height);
        return { ctx, width, height };
    };

    const roundedRect = (ctx, x, y, w, h, r) => {
        const radius = Math.max(0, Math.min(r, w / 2, h / 2));
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(x + w - radius, y);
        ctx.quadraticCurveTo(x + w, y, x + w, y + radius);
        ctx.lineTo(x + w, y + h - radius);
        ctx.quadraticCurveTo(x + w, y + h, x + w - radius, y + h);
        ctx.lineTo(x + radius, y + h);
        ctx.quadraticCurveTo(x, y + h, x, y + h - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
    };

    // Truc Y "dep": 17M -> 20M, 4.3M -> 5M
    const niceMax = (value) => {
        if (value <= 0) return 1;
        const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        const normalized = value / magnitude;
        const step = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 2.5 ? 2.5 : normalized <= 5 ? 5 : 10;
        return step * magnitude;
    };

    const emptyText = (ctx, width, height, text) => {
        ctx.fillStyle = "#94a3b8";
        ctx.font = "14px Inter, Segoe UI, Arial";
        ctx.textAlign = "center";
        ctx.fillText(text, width / 2, height / 2);
    };

    // ----- Bieu do cot -----
    const createBarChart = (canvasId, labels, values, options = {}) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return null;
        const isMoney = options.money === true;
        const allLabels = labels.slice(0, 10);
        const allValues = values.slice(0, 10).map((value) => Number(value) || 0);

        return (progress) => {
            const { ctx, width, height } = prepare(canvas, 0.6);
            if (!allValues.length || allValues.every((value) => value === 0)) {
                emptyText(ctx, width, height, "Chưa có đơn hoàn thành");
                return;
            }
            const maxBars = width < 480 ? 6 : 10;
            const visibleLabels = allLabels.slice(0, maxBars);
            const visibleValues = allValues.slice(0, maxBars);
            const padding = { top: 30, right: 14, bottom: 60, left: 50 };
            const chartWidth = width - padding.left - padding.right;
            const chartHeight = height - padding.top - padding.bottom;
            const maxValue = niceMax(Math.max(...visibleValues));
            const slotWidth = chartWidth / visibleValues.length;
            const barWidth = Math.max(12, Math.min(46, slotWidth * 0.55));
            const eased = prefersReducedMotion ? 1 : 1 - Math.pow(1 - progress, 3);

            ctx.textAlign = "right";
            for (let i = 0; i <= 4; i++) {
                const value = (maxValue * i) / 4;
                const y = padding.top + chartHeight - (chartHeight * i / 4);
                ctx.fillStyle = "#94a3b8";
                ctx.font = "11px Inter, Segoe UI, Arial";
                ctx.fillText(shortNumber(value), padding.left - 8, y + 4);
                ctx.strokeStyle = i === 0 ? "rgba(148,163,184,.35)" : "rgba(148,163,184,.14)";
                ctx.beginPath();
                ctx.moveTo(padding.left, y);
                ctx.lineTo(padding.left + chartWidth, y);
                ctx.stroke();
            }

            visibleValues.forEach((value, index) => {
                const x = padding.left + index * slotWidth + (slotWidth - barWidth) / 2;
                const barHeight = chartHeight * (value / maxValue) * eased;
                const y = padding.top + chartHeight - barHeight;
                if (barHeight > 0) {
                    const gradient = ctx.createLinearGradient(0, y, 0, y + barHeight);
                    gradient.addColorStop(0, options.color || "#f43f5e");
                    gradient.addColorStop(1, options.colorDark || "#7f1d2e");
                    ctx.fillStyle = gradient;
                    roundedRect(ctx, x, y, barWidth, Math.max(barHeight, 2), 8);
                    ctx.fill();
                }
                ctx.fillStyle = "#e2e8f0";
                ctx.font = "700 11px Inter, Segoe UI, Arial";
                ctx.textAlign = "center";
                ctx.fillText(isMoney ? shortNumber(value) + " ₫" : fullNumber(value), x + barWidth / 2, Math.max(y - 6, padding.top + 10));
                ctx.fillStyle = "#94a3b8";
                ctx.font = "11px Inter, Segoe UI, Arial";
                const words = String(visibleLabels[index] || "").slice(0, 24).split(/\s+/);
                const baseY = padding.top + chartHeight + 16;
                if (words.length > 2) {
                    ctx.fillText(words.slice(0, 2).join(" "), x + barWidth / 2, baseY);
                    ctx.fillText(words.slice(2).join(" "), x + barWidth / 2, baseY + 14);
                } else {
                    ctx.fillText(words.join(" "), x + barWidth / 2, baseY);
                }
            });
        };
    };

    // ----- Bieu do tron: chu thich ben phai, so + % -----
    const createDonutChart = (canvasId, labels, values, options = {}) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return null;
        const names = options.names || {};
        const colorOf = (label, index) => (options.colors && options.colors[label]) || COLORS[index % COLORS.length];
        const isMoney = options.money === true;

        return (progress) => {
            const { ctx, width, height } = prepare(canvas, 0.62);
            const total = values.reduce((sum, value) => sum + (Number(value) || 0), 0);
            const narrow = width < 420;
            const centerX = narrow ? width / 2 : width * 0.3;
            const centerY = narrow ? height * 0.32 : height * 0.5;
            const radius = narrow ? Math.min(width * 0.26, height * 0.26) : Math.min(width * 0.22, height * 0.36);
            let angle = -Math.PI / 2;

            values.forEach((value, index) => {
                const amount = Number(value) || 0;
                const slice = total ? amount / total * Math.PI * 2 * progress : 0;
                if (slice <= 0) return;
                ctx.beginPath();
                ctx.moveTo(centerX, centerY);
                ctx.arc(centerX, centerY, radius, angle, angle + slice);
                ctx.closePath();
                ctx.fillStyle = colorOf(labels[index], index);
                ctx.fill();
                angle += slice;
            });
            ctx.beginPath();
            ctx.arc(centerX, centerY, radius * 0.6, 0, Math.PI * 2);
            ctx.fillStyle = "#151f31";
            ctx.fill();
            ctx.fillStyle = "#f8fafc";
            ctx.font = "800 " + (isMoney ? 15 : 22) + "px Inter, Segoe UI, Arial";
            ctx.textAlign = "center";
            ctx.fillText(isMoney ? shortNumber(total) + " ₫" : String(total), centerX, centerY + 6);
            ctx.fillStyle = "#cbd5e1";
            ctx.font = "12px Inter, Segoe UI, Arial";
            ctx.fillText(options.centerLabel || "tổng", centerX, centerY + 24);

            if (!total) {
                emptyText(ctx, width, narrow ? height * 0.75 : height * 0.9, options.emptyText || "Chưa có dữ liệu");
                return;
            }

            // Chu thich
            const legendX = narrow ? 16 : width * 0.58;
            const legendTop = narrow ? centerY + radius + 28 : centerY - (labels.length - 1) * 14;
            const rowGap = 28;
            labels.forEach((label, index) => {
                const amount = Number(values[index]) || 0;
                const y = legendTop + index * rowGap;
                ctx.fillStyle = colorOf(label, index);
                roundedRect(ctx, legendX, y - 9, 12, 12, 3);
                ctx.fill();
                ctx.textAlign = "left";
                ctx.fillStyle = "#cbd5e1";
                ctx.font = "12px Inter, Segoe UI, Arial";
                ctx.fillText(String(names[label] || label).slice(0, 18), legendX + 20, y + 1);
                ctx.textAlign = "right";
                ctx.fillStyle = "#f8fafc";
                ctx.font = "700 12px Inter, Segoe UI, Arial";
                const pct = total ? Math.round(amount / total * 100) : 0;
                ctx.fillText((isMoney ? shortNumber(amount) + " ₫" : String(amount)) + " · " + pct + "%", width - 14, y + 1);
            });
        };
    };

    // ----- Bieu do doanh thu theo ngay: cot doanh thu + duong so don -----
    const createDailyChart = (canvasId, labels, revenue, orders) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return null;
        const rev = revenue.map((value) => Number(value) || 0);
        const cnt = orders.map((value) => Number(value) || 0);

        return (progress) => {
            const narrow = canvas.clientWidth < 600;
            const { ctx, width, height } = prepare(canvas, narrow ? 0.7 : 0.36);
            const padding = { top: 24, right: 44, bottom: 34, left: 52 };
            const chartWidth = width - padding.left - padding.right;
            const chartHeight = height - padding.top - padding.bottom;
            const maxRevenue = niceMax(Math.max(...rev, 0));
            // Truc so don: boi so cua 4 de 4 vach chia deu ra so nguyen
            const maxOrders = Math.max(4, Math.ceil(Math.max(...cnt, 0) / 4) * 4);
            const n = labels.length || 1;
            const slot = chartWidth / n;
            const eased = prefersReducedMotion ? 1 : 1 - Math.pow(1 - progress, 3);

            // Luoi + truc trai (tien)
            for (let i = 0; i <= 4; i++) {
                const y = padding.top + chartHeight - (chartHeight * i / 4);
                ctx.strokeStyle = i === 0 ? "rgba(148,163,184,.35)" : "rgba(148,163,184,.12)";
                ctx.beginPath();
                ctx.moveTo(padding.left, y);
                ctx.lineTo(padding.left + chartWidth, y);
                ctx.stroke();
                ctx.fillStyle = "#94a3b8";
                ctx.font = "11px Inter, Segoe UI, Arial";
                ctx.textAlign = "right";
                ctx.fillText(shortNumber(maxRevenue * i / 4), padding.left - 8, y + 4);
                ctx.textAlign = "left";
                ctx.fillStyle = "#38bdf8";
                ctx.fillText(String(Math.round(maxOrders * i / 4)), padding.left + chartWidth + 8, y + 4);
            }

            // Cot doanh thu
            const barWidth = Math.max(3, Math.min(18, slot * 0.6));
            rev.forEach((value, index) => {
                const x = padding.left + index * slot + (slot - barWidth) / 2;
                const h = chartHeight * (value / maxRevenue) * eased;
                if (h > 0) {
                    ctx.fillStyle = "#f43f5e";
                    roundedRect(ctx, x, padding.top + chartHeight - h, barWidth, Math.max(h, 2), 3);
                    ctx.fill();
                }
            });

            // Duong so don
            ctx.strokeStyle = "#38bdf8";
            ctx.lineWidth = 2;
            ctx.beginPath();
            cnt.forEach((value, index) => {
                const x = padding.left + index * slot + slot / 2;
                const y = padding.top + chartHeight - chartHeight * (value / maxOrders) * eased;
                if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
            });
            ctx.stroke();
            cnt.forEach((value, index) => {
                if (!value) return;
                const x = padding.left + index * slot + slot / 2;
                const y = padding.top + chartHeight - chartHeight * (value / maxOrders) * eased;
                ctx.fillStyle = "#38bdf8";
                ctx.beginPath();
                ctx.arc(x, y, 3, 0, Math.PI * 2);
                ctx.fill();
            });
            ctx.lineWidth = 1;

            // Nhan ngay: moi 5 ngay tren desktop, moi 10 ngay tren mobile
            const every = narrow ? 10 : 5;
            ctx.fillStyle = "#94a3b8";
            ctx.font = "11px Inter, Segoe UI, Arial";
            ctx.textAlign = "center";
            labels.forEach((label, index) => {
                if (index % every === 0 || index === labels.length - 1) {
                    ctx.fillText(label, padding.left + index * slot + slot / 2, padding.top + chartHeight + 18);
                }
            });

            if (rev.every((value) => value === 0) && cnt.every((value) => value === 0)) {
                emptyText(ctx, width, height, "Chưa có đơn trong 30 ngày qua");
            }
        };
    };

    const charts = [
        createDailyChart("dailyChart", data.dailyLabels || [], data.dailyRevenue || [], data.dailyOrders || []),
        createDonutChart("orderStatusChart", data.orderStatusLabels || [], data.orderStatusData || [], {
            names: STATUS_NAMES, colors: STATUS_COLORS, centerLabel: "đơn hàng", emptyText: "Chưa có đơn hàng"
        }),
        createDonutChart("categoryChart", data.categoryLabels || [], data.categoryData || [], {
            money: true, centerLabel: "đã thu", emptyText: "Chưa có đơn hoàn thành"
        }),
        createBarChart("salesChart", data.salesLabels || [], data.salesData || [], {
            color: "#f43f5e", colorDark: "#7f1d2e", money: true
        })
    ].filter(Boolean);

    if (charts.length) {
        if (prefersReducedMotion) {
            charts.forEach((draw) => draw(1));
        } else {
            const duration = 1100;
            const start = performance.now();
            const frame = (now) => {
                const progress = Math.min((now - start) / duration, 1);
                charts.forEach((draw) => draw(progress));
                if (progress < 1) {
                    requestAnimationFrame(frame);
                }
            };
            requestAnimationFrame(frame);
        }
        let resizeTimer = null;
        window.addEventListener("resize", () => {
            if (resizeTimer) clearTimeout(resizeTimer);
            resizeTimer = setTimeout(() => charts.forEach((draw) => draw(1)), 150);
        });
    }

    // ----- Danh dau muc dang xem tren thanh dieu huong -----
    const navLinks = Array.from(document.querySelectorAll(".admin-nav a"));
    const sections = navLinks.map((link) => document.querySelector(link.getAttribute("href"))).filter(Boolean);
    if (sections.length && typeof IntersectionObserver !== "undefined") {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                navLinks.forEach((link) => link.classList.toggle("is-active", link.getAttribute("href") === "#" + entry.target.id));
            });
        }, { rootMargin: "-40% 0px -55% 0px" });
        sections.forEach((section) => observer.observe(section));
    }
});
