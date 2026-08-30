document.addEventListener("DOMContentLoaded", () => {
    const data = window.adminChartData || {};
    const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

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
        // Server dinh dang nghin bang dau phay (5,834) -> bo phay, giu nguuyen gia tri
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

    const drawBarChart = (canvasId, labels, values, options = {}) => {
        const canvas = document.getElementById(canvasId);
        if (!canvas) {
            return;
        }

        const ctx = canvas.getContext("2d");
        const width = canvas.width;
        const height = canvas.height;
        const padding = { top: 40, right: 30, bottom: 85, left: 65 };
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        const visibleLabels = labels.slice(0, 10);
        const visibleValues = values.slice(0, 10).map((value) => Number(value) || 0);

        // Find max and min properly
        const maxDataValue = Math.max(...visibleValues, 1);
        const maxRounded = Math.pow(10, Math.ceil(Math.log10(maxDataValue)));
        const maxValue = maxRounded > maxDataValue * 1.5 ? maxRounded / 2 : maxRounded;

        const slotWidth = chartWidth / Math.max(visibleValues.length, 1);
        const barWidth = Math.max(24, Math.min(50, slotWidth * 0.45));
        canvas.title = options.title || "";
        canvas.style.cursor = "crosshair";
        const baseColor = options.color || "#ed1b2f";
        const accentColor = options.colorDark || "#0b4ea2";
        const duration = 1300;
        const start = performance.now();

        const roundedRect = (x, y, w, h, r) => {
            const radius = Math.min(r, w / 2, h / 2);
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
            ctx.clearRect(0, 0, width, height);

            const bg = ctx.createLinearGradient(0, 0, 0, height);
            bg.addColorStop(0, "#f8fafc");
            bg.addColorStop(1, "#ffffff");
            ctx.fillStyle = bg;
            ctx.fillRect(0, 0, width, height);

            ctx.strokeStyle = "#e4e7ec";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(padding.left, padding.top);
            ctx.lineTo(padding.left, padding.top + chartHeight);
            ctx.lineTo(padding.left + chartWidth, padding.top + chartHeight);
            ctx.stroke();

            ctx.fillStyle = "#667085";
            ctx.font = "12px Segoe UI, Arial";
            ctx.textAlign = "right";

            // Determine Y-axis labels and lines
            const ySteps = 4;
            for (let i = 0; i <= ySteps; i++) {
                // Calculate grid line value based on percentage of max
                const value = (maxValue * i) / ySteps;
                let displayValue = value;

                // Format nicely for large numbers
                if (value >= 1000000) {
                    displayValue = (value / 1000000).toFixed(value % 1000000 === 0 ? 0 : 1) + "M";
                } else if (value >= 1000) {
                    displayValue = (value / 1000).toFixed(value % 1000 === 0 ? 0 : 1) + "K";
                } else {
                    displayValue = Math.round(value).toString();
                }

                const y = padding.top + chartHeight - (chartHeight * i / ySteps);
                ctx.fillText(displayValue, padding.left - 10, y + 4);

                // Draw grid line
                ctx.strokeStyle = i === 0 ? "#d0d5dd" : "#eef2f6";
                ctx.beginPath();
                ctx.moveTo(padding.left, y);
                ctx.lineTo(padding.left + chartWidth, y);
                ctx.stroke();
            }

            visibleValues.forEach((value, index) => {
                const slotX = padding.left + index * slotWidth;
                const x = slotX + (slotWidth - barWidth) / 2;
                const eased = prefersReducedMotion ? 1 : 1 - Math.pow(1 - progress, 3);

                // Use square root scale to soften extreme differences without breaking zero values
                // log scale breaks at 0, sqrt is continuous and compresses large values
                const barHeight = chartHeight * Math.sqrt(value / maxValue) * eased;
                const y = padding.top + chartHeight - barHeight;
                const barGradient = ctx.createLinearGradient(0, y, 0, y + barHeight);
                barGradient.addColorStop(0, baseColor);
                barGradient.addColorStop(1, accentColor);

                ctx.fillStyle = "rgba(15, 23, 42, .04)";
                roundedRect(x + 5, padding.top + 8, barWidth - 10, chartHeight - 8, 12);
                ctx.fill();

                ctx.fillStyle = barGradient;
                roundedRect(x, y, barWidth, barHeight, 12);
                ctx.fill();

                ctx.fillStyle = "#0f172a";
                ctx.font = "700 11px Segoe UI, Arial";
                ctx.textAlign = "center";
                ctx.fillText(Number(value).toLocaleString("vi-VN"), x + barWidth / 2, Math.max(y - 8, padding.top + 12));

                ctx.fillStyle = "#475467";
                ctx.font = "11px Segoe UI, Arial";
                ctx.textAlign = "center";
                const label = String(visibleLabels[index] || "").slice(0, 20);
                const words = label.split(/\s+/);
                if (words.length > 2) {
                    ctx.fillText(words.slice(0, 2).join(" "), x + barWidth / 2, padding.top + chartHeight + 18);
                    ctx.fillText(words.slice(2).join(" "), x + barWidth / 2, padding.top + chartHeight + 32);
                } else {
                    ctx.fillText(label, x + barWidth / 2, padding.top + chartHeight + 20);
                }
            });

            if (!visibleValues.length) {
                ctx.fillStyle = "#667085";
                ctx.font = "14px Segoe UI, Arial";
                ctx.textAlign = "center";
                ctx.fillText("Chưa có dữ liệu", width / 2, height / 2);
            }
        };

        if (prefersReducedMotion) {
            draw(1);
            return;
        }

        const frame = (now) => {
            const progress = Math.min((now - start) / duration, 1);
            draw(progress);

            if (progress < 1) {
                requestAnimationFrame(frame);
            }
        };

        requestAnimationFrame(frame);
    };

    drawBarChart("salesChart", data.salesLabels || [], data.salesData || [], {
        color: "#ef1828",
        colorDark: "#8d1020"
    });

    drawBarChart("stockChart", data.stockLabels || [], data.stockData || [], {
        color: "#0b4ea2",
        colorDark: "#09845f"
    });
});
