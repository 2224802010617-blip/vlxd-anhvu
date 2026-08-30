document.addEventListener("DOMContentLoaded", () => {
    const data = window.adminChartData || {};
    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    // ----- Hien dan khi cuon -----
    const revealItems = Array.from(document.querySelectorAll(".reveal-on-scroll"));
    if (revealItems.length) {
        if (prefersReducedMotion || typeof IntersectionObserver === "undefined") {
            revealItems.forEach((item) => item.classList.add("is-visible"));
        } else {
            const observer = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (!entry.isIntersecting) {
                        return;
                    }
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                });
            }, { threshold: 0.16 });

            revealItems.forEach((item) => observer.observe(item));
        }
    }

    // ----- Dem so -----
    const formatAnimatedValue = (target, rawText, progress) => {
        const hasDecimal = rawText.includes(".");
        const value = target * progress;
        if (hasDecimal) {
            return value.toFixed(1);
        }
        return Math.round(value).toLocaleString("vi-VN");
    };

    const countElements = Array.from(document.querySelectorAll("[data-countup]"));
    countElements.forEach((el, index) => {
        const rawText = (el.textContent || "0").trim();
        // Server dinh dang nghin bang dau phay (5,834) -> bo phay, giu nguyen gia tri
        const target = Number(String(rawText).replace(/,/g, "")) || 0;

        el.textContent = "0";
        el.style.setProperty("--count-delay", `${index * 90}ms`);

        if (prefersReducedMotion) {
            el.textContent = formatAnimatedValue(target, rawText, 1);
            return;
        }

        const duration = 1100;
        const start = performance.now();

        const tick = (now) => {
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            el.textContent = formatAnimatedValue(target, rawText, eased);

            if (progress < 1) {
                requestAnimationFrame(tick);
            }
        };

        requestAnimationFrame(tick);
    });

    // ----- Bieu do cot: responsive, sac net tren mobile (high-DPI), tong toi khop dashboard -----
    const createBarChart = (canvasId, labels, values, options = {}) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas) {
            return null;
        }

        const ctx = canvas.getContext("2d");
        const baseColor = options.color || "#f43f5e";
        const accentColor = options.colorDark || "#0b4ea2";

        const allLabels = labels.slice(0, 10);
        const allValues = values.slice(0, 10).map((value) => Number(value) || 0);

        const roundedRect = (x, y, w, h, r) => {
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

        const draw = (progress) => {
            // Kich thuoc hien thi thuc te (CSS px) + he so man hinh retina
            const dpr = window.devicePixelRatio || 1;
            const cssWidth = Math.max(240, canvas.clientWidth || 560);
            const cssHeight = Math.max(260, Math.round(cssWidth * 0.6));
            const targetW = Math.round(cssWidth * dpr);
            const targetH = Math.round(cssHeight * dpr);
            if (canvas.width !== targetW) {
                canvas.width = targetW;
            }
            if (canvas.height !== targetH) {
                canvas.height = targetH;
            }
            canvas.style.height = cssHeight + "px";
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

            const width = cssWidth;
            const height = cssHeight;

            // Man hinh hep -> hien it cot hon cho de doc
            const maxBars = width < 480 ? 6 : 10;
            const visibleLabels = allLabels.slice(0, maxBars);
            const visibleValues = allValues.slice(0, maxBars);

            const padding = { top: 34, right: 18, bottom: 74, left: 54 };
            const chartWidth = width - padding.left - padding.right;
            const chartHeight = height - padding.top - padding.bottom;

            const maxDataValue = Math.max(...visibleValues, 1);
            const maxRounded = Math.pow(10, Math.ceil(Math.log10(maxDataValue)));
            const maxValue = maxRounded > maxDataValue * 1.5 ? maxRounded / 2 : maxRounded;

            const slotWidth = chartWidth / Math.max(visibleValues.length, 1);
            const barWidth = Math.max(12, Math.min(46, slotWidth * 0.5));
            const eased = prefersReducedMotion ? 1 : 1 - Math.pow(1 - progress, 3);

            ctx.clearRect(0, 0, width, height);

            // Truc
            ctx.strokeStyle = "rgba(148, 163, 184, .35)";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(padding.left, padding.top);
            ctx.lineTo(padding.left, padding.top + chartHeight);
            ctx.lineTo(padding.left + chartWidth, padding.top + chartHeight);
            ctx.stroke();

            // Vach + nhan truc Y (tuyen tinh - khop chieu cao cot)
            const ySteps = 4;
            ctx.textAlign = "right";
            for (let i = 0; i <= ySteps; i++) {
                const value = (maxValue * i) / ySteps;
                let displayValue;
                if (value >= 1000000) {
                    displayValue = (value / 1000000).toFixed(value % 1000000 === 0 ? 0 : 1) + "M";
                } else if (value >= 1000) {
                    displayValue = (value / 1000).toFixed(value % 1000 === 0 ? 0 : 1) + "K";
                } else {
                    displayValue = Math.round(value).toString();
                }

                const y = padding.top + chartHeight - (chartHeight * i / ySteps);
                ctx.fillStyle = "#94a3b8";
                ctx.font = "12px Inter, Segoe UI, Arial";
                ctx.fillText(displayValue, padding.left - 8, y + 4);

                ctx.strokeStyle = i === 0 ? "rgba(148, 163, 184, .35)" : "rgba(148, 163, 184, .14)";
                ctx.beginPath();
                ctx.moveTo(padding.left, y);
                ctx.lineTo(padding.left + chartWidth, y);
                ctx.stroke();
            }

            visibleValues.forEach((value, index) => {
                const slotX = padding.left + index * slotWidth;
                const x = slotX + (slotWidth - barWidth) / 2;

                // Thang tuyen tinh: chieu cao cot dung ti le voi gia tri va khop vach truc
                const barHeight = chartHeight * (value / maxValue) * eased;
                const y = padding.top + chartHeight - barHeight;

                // Ranh mo phia sau cot
                ctx.fillStyle = "rgba(148, 163, 184, .08)";
                roundedRect(x, padding.top, barWidth, chartHeight, 10);
                ctx.fill();

                if (barHeight > 0) {
                    const barGradient = ctx.createLinearGradient(0, y, 0, y + barHeight);
                    barGradient.addColorStop(0, baseColor);
                    barGradient.addColorStop(1, accentColor);
                    ctx.fillStyle = barGradient;
                    roundedRect(x, y, barWidth, Math.max(barHeight, 2), 10);
                    ctx.fill();
                }

                // Gia tri tren dinh cot
                ctx.fillStyle = "#e2e8f0";
                ctx.font = "700 11px Inter, Segoe UI, Arial";
                ctx.textAlign = "center";
                ctx.fillText(Number(value).toLocaleString("vi-VN"), x + barWidth / 2, Math.max(y - 7, padding.top + 11));

                // Nhan truc X (xuong dong toi da 2 dong)
                ctx.fillStyle = "#94a3b8";
                ctx.font = "11px Inter, Segoe UI, Arial";
                const label = String(visibleLabels[index] || "").slice(0, 22);
                const words = label.split(/\s+/);
                if (words.length > 2) {
                    ctx.fillText(words.slice(0, 2).join(" "), x + barWidth / 2, padding.top + chartHeight + 18);
                    ctx.fillText(words.slice(2).join(" "), x + barWidth / 2, padding.top + chartHeight + 32);
                } else {
                    ctx.fillText(label, x + barWidth / 2, padding.top + chartHeight + 20);
                }
            });

            if (!visibleValues.length || visibleValues.every((value) => value === 0)) {
                ctx.fillStyle = "#94a3b8";
                ctx.font = "14px Inter, Segoe UI, Arial";
                ctx.textAlign = "center";
                ctx.fillText("Chưa có dữ liệu", width / 2, height / 2);
            }
        };

        return draw;
    };

    const charts = [
        createBarChart("salesChart", data.salesLabels || [], data.salesData || [], {
            color: "#f43f5e",
            colorDark: "#7f1d2e"
        }),
        createBarChart("stockChart", data.stockLabels || [], data.stockData || [], {
            color: "#38bdf8",
            colorDark: "#0b4ea2"
        })
    ].filter(Boolean);

    if (charts.length) {
        if (prefersReducedMotion) {
            charts.forEach((draw) => draw(1));
        } else {
            const duration = 1300;
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

        // Ve lai (tinh) khi doi kich thuoc/xoay man hinh -> luon sac net & vua khung
        let resizeTimer = null;
        window.addEventListener("resize", () => {
            if (resizeTimer) {
                clearTimeout(resizeTimer);
            }
            resizeTimer = setTimeout(() => charts.forEach((draw) => draw(1)), 150);
        });
    }
});
