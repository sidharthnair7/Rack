// Mock API for processing clothes and AI try-on

export const processImage = async (imageFile) => {
  // Simulate network delay
  await new Promise(resolve => setTimeout(resolve, 2500));

  return {
    pricing: [
      {
        title: "Vintage Denim Jacket",
        price: "$45.00",
        source: "eBay",
        image: "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=500&q=80",
        link: "#",
      },
      {
        title: "Classic Blue Jean Jacket",
        price: "$52.99",
        source: "Poshmark",
        image: "https://images.unsplash.com/photo-1495105787522-5334e3ffa0eb?w=500&q=80",
        link: "#",
      },
      {
        title: "Levi's Trucker Jacket",
        price: "$65.00",
        source: "Grailed",
        image: "https://images.unsplash.com/photo-1551537482-f209bfc73dd1?w=500&q=80",
        link: "#",
      }
    ],
    aiTryOn: {
      modelImage: "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&q=80", // Using a stock photo of a model as a mock
      description: "AI Generated Virtual Try-On",
    }
  };
};
