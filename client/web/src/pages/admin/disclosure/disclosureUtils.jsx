export function formatDisclosureDate(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString("ko-KR");
}

export function getDisclosureCategoryLabel(value) {
  const labels = {
    BUILDING: "일반",
    DIVIDEND: "배당",
    ETC: "일반",
  };
  return labels[value] ?? value ?? "-";
}

export function getDisclosureDeletedLabel(deletedAt) {
  return deletedAt ? "삭제" : "정상";
}

export function mapDisclosureListItem(item) {
  return {
    disclosureId: item?.disclosureId ?? null,
    assetId: item?.assetId ?? null,
    disclosureTitle: item?.disclosureTitle ?? "",
    disclosureContent: item?.disclosureContent ?? "",
    disclosureCategory: item?.disclosureCategory ?? "",
    assetName: item?.assetName ?? "",
    imgUrl: item?.imgUrl ?? "",
    originName: item?.originName ?? "",
    storedName: item?.storedName ?? "",
    createdAt: item?.createdAt ?? "",
    deletedAt: item?.deletedAt ?? item?.deleteAt ?? null,
    file: null,
  };
}

export function filterDisclosures(disclosures, disclosureTypeTab, searchTerm) {
  const keyword = searchTerm.toLowerCase();
  return disclosures.filter((item) => {
    const categoryLabel = getDisclosureCategoryLabel(item.disclosureCategory).toLowerCase();
    const matchesTypeTab =
      disclosureTypeTab === "전체" ||
      getDisclosureCategoryLabel(item.disclosureCategory) === disclosureTypeTab;
    const matchesSearch =
      item.assetName.toLowerCase().includes(keyword) ||
      item.disclosureTitle.toLowerCase().includes(keyword) ||
      categoryLabel.includes(keyword);
    return matchesTypeTab && matchesSearch;
  });
}
