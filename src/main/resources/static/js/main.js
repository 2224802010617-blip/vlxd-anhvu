document.addEventListener("DOMContentLoaded", () => {
    document.documentElement.classList.add("js-anim");

    const items = document.querySelectorAll(".animate-in, .product-card, .showroom-card, .smart-recommend-grid article, .smart-hint-grid article, .criteria-grid article, .testimonial-grid article, .business-grid article, .process-grid article, .payment-section, .calculator-panel, .order-section, .stats-section, .section-heading");

    const hasAOS = typeof window.AOS !== "undefined";

    if (hasAOS) {
        // ==== Dung thu vien AOS: hieu ung cuon muot, ro rang giong web tham khao ====
        items.forEach((item) => {
            // Bo class reveal cu de tranh xung dot CSS (opacity/transform trung nhau)
            item.classList.remove("animate-in");

            // Chon kieu animation theo loai khoi cho phong phu (hoanh trang hon)
            if (!item.hasAttribute("data-aos")) {
                let effect = "fade-up";
                if (item.classList.contains("product-card") || item.classList.contains("showroom-card")) {
                    effect = "zoom-in";           // the san pham/danh muc: phong to
                } else if (item.classList.contains("section-heading")) {
                    effect = "fade-down";         // tieu de: do xuong
                } else if (item.classList.contains("stats-section")) {
                    effect = "fade-up";
                } else if (item.matches(".testimonial-grid article, .criteria-grid article, .business-grid article, .process-grid article")) {
                    effect = "flip-up";           // the danh gia/tieu chi: lat len
                }
                item.setAttribute("data-aos", effect);
            }

            // Stagger: cac the cung mot hang hien lan luot cho co chieu sau
            const parent = item.parentElement;
            if (parent) {
                const sibs = Array.prototype.filter.call(parent.children, (el) =>
                    el.hasAttribute("data-aos") ||
                    el.classList.contains("product-card") ||
                    el.classList.contains("showroom-card"));
                const idx = sibs.indexOf(item);
                if (idx > 0) {
                    item.setAttribute("data-aos-delay", String(Math.min(idx * 110, 440)));
                }
            }
        });
        window.AOS.init({
            duration: 900,
            easing: "ease-out-cubic",
            once: true,
            offset: 40,               // kich hoat som hon -> de thay hon
            anchorPlacement: "top-bottom"
        });
        // Anh/section load lai chieu cao (vd anh san pham) -> refresh de AOS tinh dung moc
        window.addEventListener("load", () => window.AOS.refresh());
    } else if ("IntersectionObserver" in window) {
        // ==== Fallback: co che tu viet (khi AOS khong tai duoc) ====
        const markVisible = (observer, entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        };

        const observer = new IntersectionObserver(
            (entries) => markVisible(observer, entries),
            { threshold: 0.12 }
        );
        const tallObserver = new IntersectionObserver(
            (entries) => markVisible(tallObserver, entries),
            { threshold: 0 }
        );

        items.forEach((item, index) => {
            item.classList.add("animate-in");
            item.style.transitionDelay = `${Math.min(index * 35, 260)}ms`;
            if (item.getBoundingClientRect().height > window.innerHeight * 0.9) {
                tallObserver.observe(item);
            } else {
                observer.observe(item);
            }
        });
    } else {
        items.forEach((item) => item.classList.add("is-visible"));
    }

    const searchForm = document.querySelector(".global-search");
    const searchInput = document.querySelector("#productSearchInput");

    if (searchForm && searchInput) {
        const normalize = (value) => value.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

        // Khung goi y (tao dong, khong can server render)
        const box = document.createElement("div");
        box.className = "search-suggest";
        box.setAttribute("role", "listbox");
        searchForm.appendChild(box);

        let products = [];
        let currentMatches = [];
        let items = [];
        let activeIndex = -1;
        let loaded = false;

        const loadProducts = () => {
            if (loaded) {
                return Promise.resolve();
            }
            loaded = true;
            return fetch("/api/chat/products")
                .then((res) => (res.ok ? res.json() : []))
                .then((data) => { products = Array.isArray(data) ? data : []; })
                .catch(() => { products = []; });
        };

        const money = (value) => {
            const n = Number(value);
            return n ? n.toLocaleString("vi-VN") + " \u20ab" : "";
        };

        const closeBox = () => {
            searchForm.classList.remove("is-suggesting");
            box.innerHTML = "";
            items = [];
            currentMatches = [];
            activeIndex = -1;
        };

        const choose = (product) => {
            if (!product) {
                return;
            }
            searchInput.value = product.name;
            closeBox();
            searchForm.submit();
        };

        const render = () => {
            const keyword = normalize(searchInput.value.trim());
            currentMatches = products.filter((p) => {
                const hay = normalize(`${p.name || ""} ${p.category || ""}`);
                return !keyword || hay.includes(keyword);
            }).slice(0, 7);

            box.innerHTML = "";
            items = [];
            activeIndex = -1;

            if (!currentMatches.length || document.activeElement !== searchInput) {
                searchForm.classList.remove("is-suggesting");
                return;
            }

            currentMatches.forEach((p) => {
                const el = document.createElement("button");
                el.type = "button";
                el.className = "search-suggest__item";
                el.setAttribute("role", "option");
                const name = document.createElement("span");
                name.className = "search-suggest__name";
                name.textContent = p.name;
                const meta = document.createElement("span");
                meta.className = "search-suggest__meta";
                meta.textContent = [p.category, money(p.price)].filter(Boolean).join(" \u00b7 ");
                el.appendChild(name);
                el.appendChild(meta);
                el.addEventListener("mousedown", (event) => {
                    event.preventDefault();
                    choose(p);
                });
                box.appendChild(el);
                items.push(el);
            });
            searchForm.classList.add("is-suggesting");
        };

        const setActive = (idx) => {
            if (!items.length) {
                return;
            }
            activeIndex = (idx + items.length) % items.length;
            items.forEach((el, i) => el.classList.toggle("is-active", i === activeIndex));
            items[activeIndex].scrollIntoView({ block: "nearest" });
        };

        searchInput.addEventListener("focus", () => { loadProducts().then(render); });
        searchInput.addEventListener("input", () => { loadProducts().then(render); });
        searchInput.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeBox();
                searchInput.blur();
                return;
            }
            if (!items.length) {
                return;
            }
            if (event.key === "ArrowDown") {
                event.preventDefault();
                setActive(activeIndex + 1);
            } else if (event.key === "ArrowUp") {
                event.preventDefault();
                setActive(activeIndex - 1);
            } else if (event.key === "Enter" && activeIndex >= 0) {
                event.preventDefault();
                choose(currentMatches[activeIndex]);
            }
        });

        document.addEventListener("click", (event) => {
            if (!searchForm.contains(event.target)) {
                closeBox();
            }
        });
    }

    const smartChips = document.querySelectorAll(".smart-chip");
    smartChips.forEach((chip) => {
        chip.addEventListener("click", () => {
            const keyword = chip.dataset.smartSearch || chip.textContent.trim();
            const input = document.querySelector("#productSearchInput");
            const form = document.querySelector(".global-search");
            if (input && form) {
                input.value = keyword;
                form.submit();
                return;
            }
            const target = document.querySelector("#products");
            if (target) {
                target.scrollIntoView({ behavior: "smooth", block: "start" });
            }
        });
    });

    const chatToggle = document.querySelector("#chatToggle");
    const chatPanel = document.querySelector("#chatPanel");
    const chatClose = document.querySelector("#chatClose");
    const chatForm = document.querySelector("#chatForm");
    const chatInput = document.querySelector("#chatInput");
    const chatMessages = document.querySelector("#chatMessages");
    const quickButtons = document.querySelectorAll(".chat-quick button");

    if (chatToggle && chatPanel && chatForm && chatInput && chatMessages) {
        const decodeChat = (value) => {
            const textarea = document.createElement("textarea");
            textarea.innerHTML = value;
            return textarea.value;
        };

        // Bi\u1ebfn l\u01b0u tr\u1eef s\u1ea3n ph\u1ea9m t\u1eeb API
        let productsData = [];

        // Trang thai luong tu van xay nha (hoi lai tung buoc)
        let chatState = null;

        // G\u1ecdi API l\u1ea5y d\u1eef li\u1ec7u s\u1ea3n ph\u1ea9m khi load trang
        fetch('/api/chat/products')
            .then(res => res.json())
            .then(data => {
                productsData = data;
                console.log("Chatbot loaded " + productsData.length + " products from API");
            })
            .catch(err => console.error("Error loading product data for chat:", err));

        const formatCurrency = (amount) => {
            return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
        };

        const knowledge = [
            {
                keywords: ["bao gia", "bao gia si", "gia si", "nha thau", "so luong lon"],
                answer: decodeChat("V&#7899;i &#273;&#417;n h&#224;ng s&#7889; l&#432;&#7907;ng l&#7899;n, anh/ch&#7883; n&#234;n g&#7917;i form B&#225;o gi&#225; s&#7881; k&#232;m danh s&#225;ch v&#7853;t t&#432;, s&#7889; l&#432;&#7907;ng, &#273;&#7883;a ch&#7881; giao v&#224; th&#7901;i gian c&#7847;n h&#224;ng. B&#7897; ph&#7853;n kinh doanh s&#7869; t&#7893;ng h&#7907;p v&#224; ph&#7843;n h&#7891;i m&#7913;c gi&#225; ph&#249; h&#7907;p theo kh&#7889;i l&#432;&#7907;ng c&#244;ng tr&#236;nh.")
            },
            {
                keywords: ["thanh toan", "vietqr", "mb", "chuyen khoan", "stk", "qr"],
                answer: decodeChat("Anh/ch&#7883; c&#243; th&#7875; thanh to&#225;n qua MB Bank/VietQR cho &#273;&#417;n h&#224;ng d&#432;&#7899;i 100 tri&#7879;u. Website s&#7869; t&#7841;o QR &#273;&#250;ng s&#7889; ti&#7873;n v&#224; n&#7897;i dung chuy&#7875;n kho&#7843;n. &#272;&#417;n t&#7915; 100 tri&#7879;u tr&#7903; l&#234;n vui l&#242;ng &#273;&#7871;n c&#7917;a h&#224;ng &#273;&#7875; x&#225;c nh&#7853;n.")
            },
            {
                keywords: ["cat", "cat da", "cat xay", "cat to", "cat be tong"],
                answer: decodeChat("Nh&#243;m c&#225;t hi&#7879;n c&#243; c&#225;t x&#226;y d&#7921;ng, c&#225;t x&#226;y t&#244;, c&#225;t v&#224;ng mi, c&#225;t b&#234; t&#244;ng, c&#225;t nghi&#7873;n, c&#225;t tr&#7855;ng v&#224; c&#225;t &#273;&#225; &#273;&#243;ng bao. X&#226;y t&#244; n&#234;n &#432;u ti&#234;n c&#225;t m&#7883;n; tr&#7897;n b&#234; t&#244;ng n&#234;n ch&#7885;n c&#225;t h&#7841;t v&#224;ng ho&#7863;c h&#7841;t l&#7899;n theo y&#234;u c&#7847;u c&#7845;p ph&#7889;i.")
            },
            {
                keywords: ["da", "da 1x2", "da 0x4", "da mi", "da 5x7"],
                answer: decodeChat("Nh&#243;m &#273;&#225; g&#7891;m &#273;&#225; 1x1, &#273;&#225; 1x2, &#273;&#225; 0x4, &#273;&#225; mi s&#224;ng, &#273;&#225; 4x6, &#273;&#225; 5x7 v&#224; &#273;&#225; xanh. &#272;&#225; 1x2 th&#432;&#7901;ng d&#249;ng cho b&#234; t&#244;ng; &#273;&#225; 0x4 d&#249;ng san n&#7873;n/l&#7899;p m&#243;ng; &#273;&#225; mi ph&#249; h&#7907;p l&#243;t n&#7873;n v&#224; h&#7841;ng m&#7909;c nh&#7887;.")
            },
            {
                keywords: ["xi mang", "pcb40", "nghi son", "hoang thach", "xi mang trang"],
                answer: decodeChat("Nh&#243;m xi m&#259;ng g&#7891;m PCB40, Nghi S&#417;n Premium, Ho&#224;ng Th&#7841;ch, TOP, xi m&#259;ng tr&#7855;ng Aalborg/SCG v&#224; v&#7853;t li&#7879;u ch&#7889;ng ch&#225;y. X&#226;y t&#244; d&#226;n d&#7909;ng c&#243; th&#7875; d&#249;ng xi m&#259;ng ph&#7893; th&#244;ng; h&#7841;ng m&#7909;c ho&#224;n thi&#7879;n trang tr&#237; n&#234;n xem xi m&#259;ng tr&#7855;ng.")
            },
            {
                keywords: ["thep", "sat", "d10", "thep hinh", "i300", "u200"],
                answer: decodeChat("Nh&#243;m th&#233;p g&#7891;m th&#233;p c&#226;y D10, th&#233;p tr&#242;n tr&#417;n, th&#233;p c&#226;y g&#226;n, th&#233;p h&#7897;p oval, th&#233;p h&#236;nh I/U/H. K&#7871;t c&#7845;u b&#234; t&#244;ng n&#234;n d&#249;ng th&#233;p c&#226;y g&#226;n &#273;&#250;ng quy c&#225;ch thi&#7871;t k&#7871;; khung nh&#224; x&#432;&#7903;ng n&#234;n ch&#7885;n th&#233;p h&#236;nh theo t&#7843;i tr&#7885;ng.")
            },
            {
                keywords: ["dat hang", "giao hang", "don hang", "lien he", "mua hang"],
                answer: decodeChat("&#272;&#7875; &#273;&#7863;t h&#224;ng, anh/ch&#7883; &#273;i&#7873;n t&#234;n, s&#7889; &#273;i&#7879;n tho&#7841;i, &#273;&#7883;a ch&#7881; giao, s&#7843;n ph&#7849;m, s&#7889; l&#432;&#7907;ng v&#224; ghi ch&#250; th&#7901;i gian giao. H&#7879; th&#7889;ng s&#7869; t&#237;nh ti&#7873;n, t&#7841;o QR n&#7871;u &#273;&#417;n d&#432;&#7899;i 100 tri&#7879;u v&#224; l&#432;u &#273;&#417;n cho admin theo d&#245;i.")
            },
            {
                keywords: ["dich vu", "san lap", "gian giao", "dao mong", "dao ao"],
                answer: decodeChat("Ngo&#224;i v&#7853;t li&#7879;u, c&#244;ng ty c&#243; nh&#243;m d&#7883;ch v&#7909; nh&#432; san l&#7845;p m&#7863;t b&#7857;ng, cho thu&#234; gi&#224;n gi&#225;o, &#273;&#224;o m&#243;ng, &#273;&#224;o h&#7847;m, &#273;&#224;o ao v&#224; c&#225;t &#273;&#225; &#273;&#243;ng bao. C&#225;c d&#7883;ch v&#7909; n&#224;y n&#234;n g&#7917;i y&#234;u c&#7847;u b&#225;o gi&#225; ri&#234;ng v&#236; ph&#7909; thu&#7897;c m&#7863;t b&#7857;ng v&#224; kh&#7889;i l&#432;&#7907;ng.")
            }
        ];

        const normalize = (value) => value.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");

        // ===== Helper cho luong tu van xay nha =====
        const houseTypeName = (type) => type === "cap4" ? "nh&agrave; c&#7845;p 4" : (type === "pho" ? "nh&agrave; ph&#7889;" : "nh&agrave; t&#7847;ng");

        const askLocationFooter = `<p style="font-size:12px; color:#64748b; margin:8px 0 0;">*Kh&#7889;i l&#432;&#7907;ng tham kh&#7843;o. Anh/ch&#7883; cho em xin &#273;&#7883;a ch&#7881; c&ocirc;ng tr&igrave;nh (x&atilde;/huy&#7879;n) &#273;&#7875; em t&iacute;nh th&ecirc;m ph&iacute; v&#7853;n chuy&#7875;n nha!</p>`;

        const buildEstimate = (type, area, floors) => {
            const scale = (area / 100) * (floors || 1);
            const fmt = (v) => Math.round(v).toLocaleString("vi-VN");
            const steelBase = type === "cap4" ? 3 : 4;
            let html = `<p>D&#7841; v&#7899;i ${houseTypeName(type)} kho&#7843;ng <b>${fmt(area)}m&sup2;</b>${floors > 1 ? " x " + floors + " t&#7847;ng" : ""}, kh&#7889;i l&#432;&#7907;ng v&#7853;t t&#432; &#432;&#7899;c t&iacute;nh:</p><ul style="margin:8px 0; padding-left:20px;">`;
            html += `<li><b>G&#7841;ch &#7889;ng 8x8x18:</b> ~${fmt(16000 * scale)} vi&ecirc;n</li>`;
            html += `<li><b>Xi m&#259;ng PCB40:</b> ~${fmt(300 * scale)} bao</li>`;
            html += `<li><b>C&aacute;t (x&acirc;y + b&ecirc; t&ocirc;ng + tr&aacute;t):</b> ~${fmt(70 * scale)} m3</li>`;
            html += `<li><b>&#272;&aacute; 1x2 / 4x6:</b> ~${fmt(25 * scale)} m3</li>`;
            html += `<li><b>Th&eacute;p c&acirc;y g&acirc;n:</b> ~${fmt(steelBase * scale)} t&#7845;n</li>`;
            html += `</ul>`;
            const pick = (kw) => productsData.find((p) => normalize(p.name).includes(kw));
            const rows = [pick("gach"), pick("xi mang"), pick("cat"), pick("da 1x2") || pick("da"), pick("thep")].filter(Boolean);
            if (rows.length > 0) {
                html += `<p style="margin:6px 0 4px;">Gi&aacute; tham kh&#7843;o t&#7841;i Anh V&#361;:</p>`;
                html += `<table style="width:100%; border-collapse: collapse; font-size: 13px;">`;
                html += `<tr style="background:#f1f5f9; text-align:left;"><th style="padding:5px; border:1px solid #e2e8f0;">V&#7853;t t&#432;</th><th style="padding:5px; border:1px solid #e2e8f0;">Gi&aacute;</th><th style="padding:5px; border:1px solid #e2e8f0;">&#272;VT</th></tr>`;
                rows.forEach((p) => {
                    html += `<tr><td style="padding:5px; border:1px solid #e2e8f0;">${p.name}</td><td style="padding:5px; border:1px solid #e2e8f0; color:#ef4444; font-weight:500;">${formatCurrency(p.price)}</td><td style="padding:5px; border:1px solid #e2e8f0;">${p.unit}</td></tr>`;
                });
                html += `</table>`;
            }
            return html;
        };

        const askHouseType = () => {
            let html = `<p>D&#7841; em ch&agrave;o anh/ch&#7883;! Anh/ch&#7883; &#273;&#7883;nh x&acirc;y nh&agrave; lo&#7841;i n&agrave;o &#273;&#7875; em t&#432; v&#7845;n cho &#273;&uacute;ng &#7841;?</p><ul style="margin:8px 0; padding-left:20px;">`;
            html += `<li><b>Nh&agrave; c&#7845;p 4</b></li>`;
            html += `<li><b>Nh&agrave; 1 t&#7847;ng / 2 t&#7847;ng</b></li>`;
            html += `<li><b>Nh&agrave; ph&#7889;</b></li>`;
            html += `</ul><p style="font-size:12px; color:#64748b;">Anh/ch&#7883; ch&#7885;n gi&uacute;p em, ho&#7863;c nh&#7855;n lu&ocirc;n di&#7879;n t&iacute;ch (VD: nh&agrave; c&#7845;p 4 100m2) &#273;&#7875; em t&iacute;nh kh&#7889;i l&#432;&#7907;ng v&#7853;t t&#432; + b&#7843;o gi&aacute; s&#7881; ngay &#7841;!</p>`;
            return html;
        };

        const askArea = (type) => {
            const openers = {
                "cap4": "D&#7841; nh&agrave; c&#7845;p 4 l&agrave; l&#7921;a ch&#7885;n ti&#7871;t ki&#7879;m &#273;&oacute; anh/ch&#7883;!",
                "tang": "D&#7841; nh&agrave; t&#7847;ng th&igrave; k&#7871;t c&#7845;u b&ecirc; t&ocirc;ng c&#7889;t th&eacute;p nhi&#7873;u h&#417;n nh&agrave; c&#7845;p 4 &#273;&aacute;!",
                "pho": "D&#7841; nh&agrave; ph&#7889; th&#432;&#7901;ng x&acirc;y 3-4 t&#7847;ng, ch&#7911;y&#7871;u l&agrave; b&ecirc; t&ocirc;ng c&#7889;t th&eacute;p &#273;&aacute;!"
            };
            return decodeChat((openers[type] || openers["cap4"]) + " Anh/ch&#7883; cho em xin di&#7879;n t&iacute;ch d&#7921; ki&#7871;n kho&#7843;ng bao nhi&ecirc;u m2 &#273;&#7875; em t&iacute;nh &#273;&#7883;nh m&#7913;c v&#7853;t t&#432; cho &#273;&uacute;ng nha (VD: 100m2) &#7841;!");
        };

        const addMessage = (message, type, isHtml = false) => {
            const item = document.createElement("div");
            item.className = `chat-message ${type}`;
            if (isHtml) {
                item.innerHTML = message;
            } else {
                item.textContent = message;
            }
            chatMessages.appendChild(item);
            chatMessages.scrollTop = chatMessages.scrollHeight;
        };

        const answerQuestion = (question) => {
            const normalized = normalize(question);

            // Chao hoi ngan
            const words = normalized.split(/\s+/).filter(Boolean);
            if (words.length <= 3 && words.some((w) => ["chao", "hello", "hi", "alo", "hey"].includes(w))) {
                return { text: decodeChat("D&#7841; em ch&agrave;o anh/ch&#7883;! Em l&agrave; tr&#7907; l&yacute; t&#432; v&#7845;n c&#7911;a VLXD Anh V&#361;. Anh/ch&#7883; c&#7847;n h&#7887;i gi&aacute;, t&iacute;nh &#273;&#7883;nh m&#7913;c hay &#273;&#7863;t h&agrave;ng th&igrave; nh&#7855;p c&acirc;u h&#7887;i nh&eacute;, em h&#7895; tr&#7907; ngay &#7841;!"), isHtml: false };
            }

            // ===== Luong tu van xay nha: hoi lai tung buoc nhu nhan vien that =====
            if (chatState && chatState.flow === "house") {
                const st = chatState;
                const isSmallTalk = (words.length <= 3 && words.some((w) => ["chao", "hello", "hi", "alo", "hey"].includes(w))) || (words.length <= 5 && ["cam on", "camon", "tam biet", "bye"].some((k) => normalized.includes(k)));
                const isOtherIntent = ["gia", "bao nhieu", "bang gia", "dia chi", "hotline", "van chuyen", "giao hang", "nhom hang", "dinh muc", "lien he"].some((k) => normalized.includes(k));
                const m2m = normalized.match(/(\d+)\s*(?:m2|met vuong)/);
                const fl = normalized.match(/(\d+)\s*tang/) || (normalized.includes("hai tang") ? [null, "2"] : null);

                if (st.step === "type") {
                    let type = null;
                    if (["cap 4", "cap4", "cap bon", "nha cap bon"].some((k) => normalized.includes(k))) type = "cap4";
                    else if (normalized.includes("pho")) type = "pho";
                    else if (normalized.includes("tang")) type = "tang";

                    if (type) {
                        st.type = type;
                        if (fl) st.floors = parseInt(fl[1]);
                        if (m2m) st.area = parseInt(m2m[1]);
                        if (st.area) {
                            st.step = "location";
                            return { text: buildEstimate(st.type, st.area, st.floors) + askLocationFooter, isHtml: true };
                        }
                        st.step = "area";
                        return { text: askArea(type), isHtml: false };
                    }
                    if (!isOtherIntent && !isSmallTalk) {
                        return { text: decodeChat("D&#7841; anh/ch&#7883; ch&#7885;n gi&uacute;p em: nh&agrave; c&#7845;p 4, nh&agrave; 1-2 t&#7847;ng hay nh&agrave; ph&#7889; &#7841;? Ho&#7863;c nh&#7855;n lu&ocirc;n di&#7879;n t&iacute;ch (VD: nh&agrave; c&#7845;p 4 100m2) c&#361;ng &#273;&#432;&#7907;c nha!"), isHtml: false };
                    }
                }

                if (st.step === "area") {
                    const prevType = st.type;
                    if (["cap 4", "cap4", "cap bon"].some((k) => normalized.includes(k))) st.type = "cap4";
                    else if (normalized.includes("pho")) st.type = "pho";
                    else if (normalized.includes("tang")) st.type = "tang";
                    if (fl) st.floors = parseInt(fl[1]);
                    const fm = m2m || (words.length <= 2 && !normalized.includes("trieu") ? normalized.match(/(\d+)/) : null);
                    if (fm) {
                        st.area = parseInt(fm[1]);
                        st.step = "location";
                        return { text: buildEstimate(st.type || "cap4", st.area, st.floors) + askLocationFooter, isHtml: true };
                    }
                    if (st.type !== prevType) {
                        return { text: askArea(st.type), isHtml: false };
                    }
                    if (!isOtherIntent && !isSmallTalk) {
                        return { text: decodeChat("D&#7841; anh/ch&#7883; cho em xin di&#7879;n t&iacute;ch d&#7921; ki&#7871;n kho&#7843;ng bao nhi&ecirc;u m2 lu&ocirc;n nha (VD: 100m2) &#273;&#7875; em t&iacute;nh gi&uacute;p &#7841;!"), isHtml: false };
                    }
                }

                if (st.step === "location") {
                    if (!isOtherIntent && !isSmallTalk) {
                        if (["cap 4", "cap4", "xay nha", "lam nha", "dung nha"].some((k) => normalized.includes(k))) {
                            chatState = { flow: "house", type: null, area: null, floors: 1, step: "type" };
                            return { text: askHouseType(), isHtml: true };
                        }
                        const phone = (question.match(/0\d{8,9}/) || [])[0];
                        chatState = null;
                        if (phone) {
                            return { text: decodeChat("D&#7841; em &#273;&atilde; ghi l&#7841;i s&#7889; " + phone + " r&#7891;i &#7841;! B&ecirc;n em s&#7869; g&#7885;i l&#7841;i t&#432; v&#7845;n tr&#7885;n g&ocute;i kh&#7889;i l&#432;&#7907;ng + ph&iacute; v&#7853;n chuy&#7875;n. Anh/ch&#7883; c&#7847;n g&#7845;p th&igrave; g&#7885;i lu&ocirc;n hotline 0866785645 nha!"), isHtml: false };
                        }
                        return { text: decodeChat("D&#7841; khu v&#7921;c &#273;&oacute; b&ecirc;n em giao xe t&#7843;i, xe ben t&#7899;i t&#7853;n c&ocirc;ng tr&igrave;nh &#273;&#432;&#7907;c &#7841;! Ph&iacute; v&#7853;n chuy&#7875;n em s&#7869; cho b&ecirc;n &#273;i&#7873;u &#273;&#7897; b&aacute;o ch&iacute;nh x&aacute;c theo kh&#7889;i l&#432;&#7907;ng h&agrave;ng. Anh/ch&#7883; cho em xin s&#7889; &#273;i&#7879;n tho&#7841;i ho&#7863;c g&#7885;i lu&ocirc;n hotline 0866785645, b&ecirc;n em b&aacute;o tr&#7885;n g&ocute;i ngay nha!"), isHtml: false };
                    }
                }
            }

            // Lien he / dia chi / gio lam
            if (["dia chi", "o dau", "cua hang", "hotline", "sdt", "so dien thoai", "lien he", "gio lam", "gio mo cua", "email", "zalo"].some((k) => normalized.includes(k))) {
                let html = `<p>Th&ocirc;ng tin li&ecirc;n h&#7879; c&#7911;a Anh V&#361; &#7841;:</p>`;
                html += `<ul style="margin:8px 0; padding-left:20px;">`;
                html += `<li><b>&#272;&#7883;a ch&#7881;:</b> 617 Nguy&#7877;n Hu&#7879;, P. B&igrave;nh Long, TP. &#272;&#7891;ng Nai</li>`;
                html += `<li><b>Hotline/Zalo:</b> <a href="tel:0866785645" style="color:#ef4444; font-weight:700;">0866785645</a></li>`;
                html += `<li><b>Email:</b> xaydungvantaidaphuongthucanhvu@gmail.com</li>`;
                html += `</ul><p style="font-size:12px; color:#64748b;">Anh/ch&#7883; g&#7885;i ho&#7843;c nh&#7855;n Zalo l&agrave; em b&aacute;o gi&aacute; ngay &#7841;!</p>`;
                return { text: html, isHtml: true };
            }

            // Cam on / tam biet
            if (words.length <= 5 && ["cam on", "camon", "tam biet", "bye"].some((k) => normalized.includes(k))) {
                return { text: decodeChat("D&#7841; kh&ocirc;ng c&oacute; g&igrave; &#7841;! Anh/ch&#7883; c&#7847;n h&#7895; tr&#7907; g&igrave; th&ecirc;m v&#7873; v&#7853;t li&#7879;u th&igrave; nh&#7855;n em lu&ocirc;n nha. Ch&uacute;c anh/ch&#7883; thi c&ocirc;ng thu&#7853;n l&#7907;i!"), isHtml: false };
            }

            // Van chuyen / giao hang
            if (["van chuyen", "giao hang", "phi ship", "phi van chuyen", "xe ben", "xe cau", "ba gac", "cho hang"].some((k) => normalized.includes(k))) {
                return { text: decodeChat("B&ecirc;n Anh V&#361; c&oacute; &#273;&#7847;y &#273;&#7911; xe t&#7843;i, xe ben, xe c&#7849;u v&agrave; xe ba g&aacute;c giao t&#7899;i t&#7853;n c&ocirc;ng tr&igrave;nh &#7841;. Ph&iacute; v&#7853;n chuy&#7875;n t&iacute;nh theo kho&#7843;ng c&aacute;ch v&agrave; kh&#7889;i l&#432;&#7907;ng h&agrave;ng &#8212; anh/ch&#7883; g&#7917;i &#273;&#7883;a ch&#7881;, em b&aacute;o c&#432;&#7899;c ch&iacute;nh x&aacute;c ngay &#7841;."), isHtml: false };
            }

            // Nhom hang tu DB
            if (["nhom hang", "co nhung gi", "ban nhung gi", "san pham nao", "danh muc"].some((k) => normalized.includes(k)) && productsData.length > 0) {
                const cats = [...new Set(productsData.map((p) => p.category))];
                let html = `<p>Hi&#7879;n Anh V&#361; c&oacute; c&aacute;c nh&oacute;m h&agrave;ng &#7841;:</p><ul style="margin:8px 0; padding-left:20px;">`;
                cats.forEach((c) => {
                    const count = productsData.filter((p) => p.category === c).length;
                    html += `<li><b>${c}</b> (${count} s&#7843;n ph&#7849;m)</li>`;
                });
                html += `</ul><p style="font-size:12px; color:#64748b;">Anh/ch&#7883; h&#7887;i gi&aacute; nh&oacute;m n&agrave;o (VD: 'B&aacute;o gi&aacute; th&eacute;p') em g&#7917;i b&#7843;ng gi&aacute; ngay &#7841;!</p>`;
                return { text: html, isHtml: true };
            }

            // Nha cap 4: co dien tich -> du toan ngay + hoi dia chi; chua co -> hoi dien tich truoc
            if (normalized.includes("cap 4") || normalized.includes("nha cap bon")) {
                const m2m = normalized.match(/(\d+)\s*(?:m2|met vuong)/);
                const fl = normalized.match(/(\d+)\s*tang/);
                const floors = fl ? parseInt(fl[1]) : 1;
                if (m2m) {
                    chatState = { flow: "house", type: "cap4", area: parseInt(m2m[1]), floors: floors, step: "location" };
                    return { text: buildEstimate("cap4", parseInt(m2m[1]), floors) + askLocationFooter, isHtml: true };
                }
                chatState = { flow: "house", type: "cap4", area: null, floors: 1, step: "area" };
                return { text: askArea("cap4"), isHtml: false };
            }

            // Bang dinh muc pho thong
            if (["dinh muc", "1m3 be tong", "mot met khoi", "1m2 tuong"].some((k) => normalized.includes(k))) {
                let html = `<p>B&#7843;ng &#273;&#7883;nh m&#7913;c tham kh&#7843;o &#7841;:</p><table style="width:100%; border-collapse: collapse; font-size: 13px;"><tr style="background:#f1f5f9; text-align:left;"><th style="padding:5px; border:1px solid #e2e8f0;">H&#7841;ng m&#7909;c</th><th style="padding:5px; border:1px solid #e2e8f0;">&#272;&#7883;nh m&#7913;c</th></tr>`;
                [["T&#432;&#7901;ng 10 (1m2)", "~55 vi&ecirc;n g&#7841;ch + 0.02m3 v&#7915;a"],
                 ["T&#432;&#7901;ng 20 (1m2)", "~110 vi&ecirc;n g&#7841;ch + 0.04m3 v&#7915;a"],
                 ["B&ecirc; t&ocirc;ng 1m3", "~7 bao xi m&#259;ng + 0.5m3 c&aacute;t + 0.9m3 &#273;&aacute;"],
                 ["Tr&aacute;t 1m2 (2 l&#7899;p)", "~0.5 bao xi m&#259;ng + 0.03m3 c&aacute;t"]].forEach((r) => {
                    html += `<tr><td style="padding:5px; border:1px solid #e2e8f0;">${r[0]}</td><td style="padding:5px; border:1px solid #e2e8f0;">${r[1]}</td></tr>`;
                });
                html += `</table><p style="font-size:12px; color:#64748b; margin-top:6px;">*&#272;&#7883;nh m&#7913;c tham kh&#7843;o, t&ugrave;y thi c&ocirc;ng th&#7921;c t&#7871;. Anh/ch&#7883; c&#7847;n t&iacute;nh h&#7853;u g&igrave; nh&#7855;p em t&iacute;nh gi&uacute;p &#7841;!</p>`;
                return { text: html, isHtml: true };
            }

            // Tu van chon loai nao tot
            if (["loai nao tot", "hang nao tot", "xi mang nao tot", "gach nao tot", "chon loai nao", "loai gi tot", "chat luong nao"].some((k) => normalized.includes(k))) {
                return { text: decodeChat("T&#249;y h&#7841;ng m&#7909;c &#7841;: <b>K&#7871;t c&#7845;u - m&oacute;ng - s&agrave;n</b> n&ecirc;n d&ugrave;ng xi m&#259;ng PCB40 &#273;&#7875; &#273;&#7843;m b&#7843;o l&#7921;c ch&#7883;u &#273;&#7921;ng. <b>X&acirc;y t&ocirc;</b> d&ugrave;ng xi m&#259;ng ph&#7893; th&ocirc;ng l&agrave; &#273;&#7911;, ti&#7871;t ki&#7879;m h&#417;n. <b>G&#7841;ch</b> th&igrave; g&#7841;ch &#7889;ng nung 8x8x18 b&#7873;n, c&aacute;ch nhi&#7879;t t&#7889;t. <b>&#272;&aacute;</b> ch&#7885;n theo h&#7841;ng m&#7909;c: b&ecirc; t&ocirc;ng d&ugrave;ng &#273;&aacute; 1x2, san n&#7873;n d&ugrave;ng &#273;&aacute; 0x4."), isHtml: true };
            }

            // X\u1eed l\u00fd b\u00e1o gi\u00e1 t\u1ef1 \u0111\u1ed9ng theo t\u1eeb kh\u00f3a "b\u1ea3ng gi\u00e1", "gi\u00e1", "bao nhi\u00eau" \u0111\u1ed1i v\u1edbi c\u00e1c s\u1ea3n ph\u1ea9m
            const isPriceQuery = normalized.includes("gia") || normalized.includes("bao nhieu") || normalized.includes("bang gia");

            if (isPriceQuery && productsData.length > 0) {
                // L\u1ecdc s\u1ea3n ph\u1ea9m ph\u00f9 h\u1ee3p
                let matchedProducts = [];

                // C\u00e1c keyword lo\u1ea1i s\u1ea3n ph\u1ea9m
                if (normalized.includes("gach")) matchedProducts = productsData.filter(p => normalize(p.name).includes("gach") || normalize(p.category).includes("gach"));
                else if (normalized.includes("cat")) matchedProducts = productsData.filter(p => normalize(p.name).includes("cat") || normalize(p.category).includes("cat"));
                else if (normalized.includes("da ") || normalized.includes("da 1x2") || normalized.includes("da mi")) matchedProducts = productsData.filter(p => normalize(p.name).includes("da") || normalize(p.category).includes("da"));
                else if (normalized.includes("xi mang")) matchedProducts = productsData.filter(p => normalize(p.name).includes("xi mang") || normalize(p.category).includes("xi mang"));
                else if (normalized.includes("thep") || normalized.includes("sat")) matchedProducts = productsData.filter(p => normalize(p.name).includes("thep") || normalize(p.category).includes("thep"));

                // N\u1ebfu t\u00ecm th\u1ea5y m\u1ed9t s\u1ea3n ph\u1ea9m c\u1ee5 th\u1ec3 h\u01a1n
                const words = normalized.split(/\s+/);
                const specificMatches = productsData.filter(p => {
                    const normName = normalize(p.name);
                    return words.some(w => w.length > 2 && normName.includes(w)) &&
                           (matchedProducts.length === 0 || matchedProducts.includes(p));
                });

                if (specificMatches.length > 0) {
                    matchedProducts = specificMatches;
                }

                if (matchedProducts.length > 0) {
                    let html = `<p>D\u1ea1 em g\u1eedi anh/ch\u1ecb b\u00e1o gi\u00e1 c\u00e1c s\u1ea3n ph\u1ea9m ph\u00f9 h\u1ee3p:</p>`;
                    html += `<table style="width:100%; margin-top:8px; border-collapse: collapse; font-size: 13px;">`;
                    html += `<tr style="background:#f1f5f9; text-align:left;"><th style="padding:6px; border:1px solid #e2e8f0;">S\u1ea3n ph\u1ea9m</th><th style="padding:6px; border:1px solid #e2e8f0;">Gi\u00e1</th><th style="padding:6px; border:1px solid #e2e8f0;">\u0110VT</th></tr>`;

                    matchedProducts.slice(0, 5).forEach(p => {
                        html += `<tr>`;
                        html += `<td style="padding:6px; border:1px solid #e2e8f0;">${p.name}</td>`;
                        html += `<td style="padding:6px; border:1px solid #e2e8f0; color:#ef4444; font-weight:500;">${formatCurrency(p.price)}</td>`;
                        html += `<td style="padding:6px; border:1px solid #e2e8f0;">${p.unit}</td>`;
                        html += `</tr>`;
                    });
                    html += `</table>`;

                    if (matchedProducts.length > 5) {
                        html += `<p style="font-size:12px; margin-top:6px; color:#64748b;">*V\u00e0 ${matchedProducts.length - 5} s\u1ea3n ph\u1ea9m kh\u00e1c. Vui l\u00f2ng xem \u1edf ph\u1ea7n Danh M\u1ee5c.</p>`;
                    }

                    return { text: html, isHtml: true };
                }
            }

            // X\u1eed l\u00fd t\u1ef1 \u0111\u1ed9ng t\u00ednh to\u00e1n v\u1eadt t\u01b0 (v\u00ed d\u1ee5: "100m2 t\u01b0\u1eddng c\u1ea7n bao nhi\u00eau g\u1ea1ch")
            const m2Match = normalized.match(/(\d+)\s*m2/);
            if (m2Match && productsData.length > 0) {
                const area = parseInt(m2Match[1]);
                const isWall = normalized.includes("tuong") || (normalized.includes("xay") && !normalized.includes("nha"));

                if (isWall) {
                    // L\u1ecdc g\u1ea1ch \u0111\u1ec3 t\u00ednh
                    const gach = productsData.find(p => normalize(p.category).includes("gach") || normalize(p.name).includes("gach"));
                    const xiMang = productsData.find(p => normalize(p.name).includes("xi mang"));
                    const cat = productsData.find(p => normalize(p.name).includes("cat"));

                    if (gach) {
                        // \u0110\u1ecbnh m\u1ee9c gi\u1ea3 l\u1eadp (s\u1ebd l\u1ea5y t\u1eeb DB n\u1ebfu c\u00f3 consumption)
                        const gachPerM2 = gach.consumption || 68; // 68 vi\u00ean/m2 t\u01b0\u1eddng 10
                        const totalGach = Math.ceil(area * gachPerM2);

                        let html = `<p>V\u1edbi <b>${area}m\u00b2</b> t\u01b0\u1eddng, anh/ch\u1ecb c\u1ea7n d\u1ef1 ki\u1ebfn kho\u1ea3ng:</p>`;
                        html += `<ul style="margin: 8px 0; padding-left: 20px;">`;
                        html += `<li><b>G\u1ea1ch (${gach.name}):</b> ${totalGach.toLocaleString('vi-VN')} vi\u00ean (~${formatCurrency(totalGach * gach.price)})</li>`;
                        if (xiMang) html += `<li><b>Xi m\u0103ng:</b> ~${Math.ceil(area * 0.3)} bao</li>`;
                        if (cat) html += `<li><b>C\u00e1t x\u00e2y:</b> ~${(area * 0.05).toFixed(1)} kh\u1ed1i</li>`;
                        html += `</ul>`;
                        html += `<p style="font-size: 12px; color: #64748b;">*L\u01b0u \u00fd: \u0110\u1ecbnh m\u1ee9c t\u00ednh cho t\u01b0\u1eddng 10, ch\u1ec9 mang t\u00ednh ch\u1ea5t tham kh\u1ea3o.</p>`;

                        return { text: html, isHtml: true };
                    }
                }
            }

            // Tu van xay nha: hoi lai loai nha truoc nhu nhan vien that
            const isHouseBuild = ["xay nha", "dung nha", "lam nha", "cai nha", "nha cap 4", "nha ngoi", "xay tuong", "mong nha", "hoan thien", "xay nha ngoi", "xay nha cap", "xay nha gi"].some((k) => normalized.includes(k));

            if (isHouseBuild) {
                // Neu khach da noi ro dien tich thi tinh luon
                const m2m = normalized.match(/(\d+)\s*(?:m2|met vuong)/);
                const fl = normalized.match(/(\d+)\s*tang/);
                const floors = fl ? parseInt(fl[1]) : 1;
                let type = null;
                if (["cap 4", "cap4", "cap bon"].some((k) => normalized.includes(k))) type = "cap4";
                else if (normalized.includes("pho")) type = "pho";
                else if (normalized.includes("tang")) type = "tang";

                if (m2m && type) {
                    chatState = { flow: "house", type: type, area: parseInt(m2m[1]), floors: floors, step: "location" };
                    return { text: buildEstimate(type, parseInt(m2m[1]), floors) + askLocationFooter, isHtml: true };
                }
                if (m2m) {
                    // Co dien tich nhung chua biet loai nha -> doan mac dinh cap 4, van hoi lai
                    chatState = { flow: "house", type: null, area: parseInt(m2m[1]), floors: floors, step: "type" };
                    return { text: buildEstimate("cap4", parseInt(m2m[1]), floors) + askLocationFooter, isHtml: true };
                }
                chatState = { flow: "house", type: type, area: null, floors: 1, step: type ? "area" : "type" };
                if (type) {
                    return { text: askArea(type), isHtml: false };
                }
                return { text: askHouseType(), isHtml: true };
            }

            // Fallback v\u1ec1 rule base b\u00ecnh th\u01b0\u1eddng
            const match = knowledge.find((entry) => entry.keywords.some((keyword) => normalized.includes(keyword)));
            if (match) {
                return { text: match.answer, isHtml: false };
            }

            return {
                text: decodeChat("D&#7841; em ch&#432;a hi&#7875;u h&#7887;i c&#7911;a anh/ch&#7883; l&#7855;m &#7841;. Em c&oacute; th&#7875; gi&#250;p: <br>&#8226; <b>B&aacute;o gi&aacute;</b> &#8212; h&#7887;i 'Gi&aacute; c&aacute;t', 'B&aacute;o gi&aacute; th&eacute;p'<br>&#8226; <b>T&iacute;nh &#273;&#7883;nh m&#7913;c</b> &#8212; 'X&acirc;y 100m2 c&#7847;n bao nhi&ecirc;u g&#7841;ch'<br>&#8226; <b>T&#432; v&#7845;n x&acirc;y nh&agrave;</b> &#8212; 'X&acirc;y nh&agrave; c&#7845;p 4 c&#7847;n mua g&igrave;'<br>&#8226; <b>Li&ecirc;n h&#7879;</b> &#8212; h&#7887;i '&#272;&#7883;a ch&#7883;', 'Hotline'<br>Ho&#7863;c g&#7885;i 0866785645 &#273;&#7875; nh&acirc;n vi&ecirc;n t&#432; v&#7845;n tr&#7921;c ti&#7871;p &#7841;!"),
                isHtml: true
            };
        };

        // Hien "dang go..." (3 cham nhap nhay) cho giong nguoi that
        const showTyping = () => {
            const item = document.createElement("div");
            item.className = "chat-message bot chat-typing";
            item.innerHTML = '<span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>';
            chatMessages.appendChild(item);
            chatMessages.scrollTop = chatMessages.scrollHeight;
            return item;
        };

        const ask = (question) => {
            const cleanQuestion = question.trim();
            if (!cleanQuestion) return;
            addMessage(cleanQuestion, "user");

            // Tinh truoc cau tra loi, roi hien "dang go" mot chut nhu nhan vien that
            const response = answerQuestion(cleanQuestion);
            const typing = showTyping();
            // Delay thay doi theo do dai cau tra loi (cau dai -> "go" lau hon), gioi han 600-1600ms
            const len = (response.text || "").replace(/<[^>]+>/g, "").length;
            const delay = Math.min(1600, Math.max(600, 500 + len * 6));
            window.setTimeout(() => {
                typing.remove();
                addMessage(response.text, "bot", response.isHtml);
            }, delay);
        };

        chatToggle.addEventListener("click", () => {
            chatPanel.hidden = !chatPanel.hidden;
            chatToggle.setAttribute("aria-expanded", String(!chatPanel.hidden));
            document.body.classList.toggle("chat-open", !chatPanel.hidden);
            if (!chatPanel.hidden) {
                chatInput.focus();
            }
        });

        if (chatClose) {
            chatClose.addEventListener("click", () => {
                chatPanel.hidden = true;
                chatToggle.setAttribute("aria-expanded", "false");
                document.body.classList.remove("chat-open");
            });
        }

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !chatPanel.hidden) {
                chatPanel.hidden = true;
                chatToggle.setAttribute("aria-expanded", "false");
                document.body.classList.remove("chat-open");
            }
        });

        chatForm.addEventListener("submit", (event) => {
            event.preventDefault();
            ask(chatInput.value);
            chatInput.value = "";
        });

        quickButtons.forEach((button) => {
            button.addEventListener("click", () => ask(button.dataset.question || button.textContent));
        });
    }
});
document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#smartEstimatorForm");
    const typeInput = document.querySelector("#estimateType");
    const areaInput = document.querySelector("#estimateArea");
    const budgetInput = document.querySelector("#estimateBudget");
    const result = document.querySelector("#smartEstimatorResult");
    const score = document.querySelector("#estimateScore");
    const title = document.querySelector("#estimateTitle");
    const summary = document.querySelector("#estimateSummary");
    const tags = document.querySelector("#estimateTags");

    if (!form || !typeInput || !areaInput || !budgetInput || !result || !score || !title || !summary || !tags) {
        return;
    }

    const decodeHtml = (value) => {
        const textarea = document.createElement("textarea");
        textarea.innerHTML = value;
        return textarea.value;
    };

    const plans = {
        "xay-to": {
            title: "Combo x&acirc;y t&ocirc; d&acirc;n d&#7909;ng",
            tags: ["G&#7841;ch &#7889;ng", "Xi m&#259;ng PCB40", "C&aacute;t x&acirc;y"],
            note: "Ph&ugrave; h&#7907;p cho t&#432;&#7901;ng nh&agrave;, s&#7917;a ch&#7919;a v&agrave; c&ocirc;ng tr&igrave;nh nh&#7887;. N&ecirc;n b&aacute;o gi&aacute; g&#7841;ch, xi m&#259;ng v&agrave; c&aacute;t x&acirc;y tr&#432;&#7899;c &#273;&#7875; ch&#7889;t kh&#7889;i l&#432;&#7907;ng."
        },
        "be-tong": {
            title: "Combo b&ecirc; t&ocirc;ng m&oacute;ng s&agrave;n",
            tags: ["&#272;&aacute; 1x2", "C&aacute;t b&ecirc; t&ocirc;ng", "Th&eacute;p c&acirc;y", "Xi m&#259;ng"],
            note: "Ph&ugrave; h&#7907;p cho m&oacute;ng, s&agrave;n v&agrave; c&#7845;u ki&#7879;n ch&#7883;u l&#7921;c. N&ecirc;n &#432;u ti&ecirc;n &#273;&aacute; 1x2, c&aacute;t b&ecirc; t&ocirc;ng v&agrave; th&eacute;p &#273;&uacute;ng quy c&aacute;ch thi&#7871;t k&#7871;."
        },
        "san-lap": {
            title: "Combo san l&#7845;p m&#7863;t b&#7857;ng",
            tags: ["C&aacute;t san l&#7845;p", "&#272;&aacute; 0x4", "V&#7853;n chuy&#7875;n"],
            note: "Ph&ugrave; h&#7907;p cho n&#7873;n, &#273;&#432;&#7901;ng n&#7897;i b&#7897; v&agrave; m&#7863;t b&#7857;ng thi c&ocirc;ng. N&ecirc;n g&#7917;i th&ecirc;m &#273;&#7883;a ch&#7881; v&agrave; kh&#7889;i l&#432;&#7907;ng xe &#273;&#7875; b&aacute;o gi&aacute; s&aacute;t h&#417;n."
        },
        "khung-thep": {
            title: "Combo th&eacute;p v&agrave; v&#7853;t t&#432; gia c&#432;&#7901;ng",
            tags: ["Th&eacute;p h&igrave;nh", "Th&eacute;p h&#7897;p", "Th&eacute;p c&acirc;y g&acirc;n"],
            note: "Ph&ugrave; h&#7907;p cho khung, m&aacute;i che, nh&agrave; x&#432;&#7903;ng v&agrave; h&#7841;ng m&#7909;c gia c&#432;&#7901;ng. N&ecirc;n x&aacute;c &#273;&#7883;nh chi&#7873;u d&agrave;i, quy c&aacute;ch v&agrave; t&#7843;i tr&#7885;ng tr&#432;&#7899;c khi &#273;&#7863;t."
        }
    };

    const budgetText = {
        saving: "G&oacute;i ti&#7871;t ki&#7879;m: &#432;u ti&ecirc;n v&#7853;t t&#432; ph&#7893; th&ocirc;ng, d&#7877; ki&#7875;m, d&#7877; giao nhanh.",
        standard: "G&oacute;i ti&ecirc;u chu&#7849;n: c&acirc;n b&#7857;ng gi&aacute;, t&#7891;n kho v&agrave; ch&#7845;t l&#432;&#7907;ng thi c&ocirc;ng.",
        premium: "G&oacute;i &#432;u ti&ecirc;n ch&#7845;t l&#432;&#7907;ng: ch&#7885;n nh&oacute;m v&#7853;t t&#432; &#7893;n &#273;&#7883;nh h&#417;n cho h&#7841;ng m&#7909;c quan tr&#7885;ng."
    };

    const updateEstimate = () => {
        const plan = plans[typeInput.value] || plans["xay-to"];
        const area = Math.max(Number(areaInput.value) || 1, 1);
        const budget = budgetInput.value || "standard";
        const computedScore = Math.max(76, Math.min(98, Math.round(88 + Math.min(area, 220) / 28 + (budget === "premium" ? 4 : budget === "saving" ? -2 : 1))));

        score.textContent = `${computedScore}%`;
        title.textContent = decodeHtml(plan.title);
        summary.textContent = decodeHtml(`${plan.note} Di&#7879;n t&iacute;ch t&#7841;m t&iacute;nh ${area.toLocaleString("vi-VN")} m2. ${budgetText[budget]}`);
        tags.innerHTML = plan.tags.map((item) => `<span>${item}</span>`).join("");

        result.classList.remove("is-updating");
        window.requestAnimationFrame(() => result.classList.add("is-updating"));
    };

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        updateEstimate();
        // Cuon toi ket qua (nhat la tren dien thoai panel nam ben duoi)
        if (window.matchMedia("(max-width: 900px)").matches) {
            result.scrollIntoView({ behavior: "smooth", block: "center" });
        }
        // Nhap nhay vien de nguoi dung thay ro co ket qua moi
        result.classList.add("is-flash");
        window.setTimeout(() => result.classList.remove("is-flash"), 900);
    });

    [typeInput, areaInput, budgetInput].forEach((input) => input.addEventListener("input", updateEstimate));
    updateEstimate();
});

// ===== O tim kiem: placeholder tu go chu (typewriter) cho sinh dong =====
document.addEventListener("DOMContentLoaded", () => {
    const input = document.querySelector("#productSearchInput");
    if (!input) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    const words = ["gạch ống xây tường...", "xi măng PCB40...", "cát xây, cát tô...", "đá 1x2, đá mi...", "thép cây, thép hộp...", "dịch vụ san lấp..."];
    const prefix = "Nhập từ khóa: ";
    let wordIdx = 0, charIdx = 0, deleting = false, paused = 0;

    const tick = () => {
        // Dung go khi khach dang bam vao o hoac da nhap chu
        if (document.activeElement === input || input.value) {
            input.setAttribute("placeholder", "Nhập từ khóa");
            return window.setTimeout(tick, 800);
        }
        const word = words[wordIdx];
        if (paused > 0) { paused--; return window.setTimeout(tick, 60); }

        if (!deleting) {
            charIdx++;
            if (charIdx > word.length) { deleting = true; paused = 22; }
        } else {
            charIdx--;
            if (charIdx === 0) { deleting = false; wordIdx = (wordIdx + 1) % words.length; }
        }
        input.setAttribute("placeholder", prefix + word.substring(0, charIdx));
        window.setTimeout(tick, deleting ? 45 : 90);
    };
    window.setTimeout(tick, 1200);
});